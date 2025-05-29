package com.lvonce.taitan;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.lvonce.taitan.Entity", "com.example.orm.Id", "com.example.orm.Column"})
public class OrmProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Entity.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                TypeElement typeElement = (TypeElement) element;
                generateDao(typeElement);
                generateMutation(typeElement); // 新增：生成 Mutation 类
                generateFieldExprs(typeElement);
            }
        }
        return true;
    }

    private void generateDao(TypeElement entityType) {
        String packageName = processingEnv.getElementUtils().getPackageOf(entityType).toString();
        String entityName = entityType.getSimpleName().toString();
        String daoName = entityName + "Dao";
        String tableName = getTableName(entityType);

        // 收集字段信息
        List<FieldInfo> fields = new ArrayList<>();
        VariableElement primaryKey = null;
        for (Element enclosed : entityType.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosed;
                boolean isId = field.getAnnotation(Id.class) != null;
                Column column = field.getAnnotation(Column.class);
                String columnName = column != null && !column.name().isEmpty() ? column.name() : field.getSimpleName().toString();
                boolean isAutoIncrement = column != null && column.isAutoIncrement();
                fields.add(new FieldInfo(field, columnName, isId, isAutoIncrement));
                if (isId) primaryKey = field;
            }
        }

        // 使用JavaPoet生成DAO类
        TypeSpec dao = TypeSpec.classBuilder(daoName)
                .addModifiers(Modifier.PUBLIC)
                .superclass(ClassName.get("com.lvonce.taitan", "Executor"))
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ClassName.get("javax.sql", "DataSource"), "dataSource")
                        .addStatement("super(dataSource)")
                        .build())
                .addMethod(generateInsert(entityType, tableName, fields))
                .addMethod(generateFindById(entityType, tableName, primaryKey, fields))
                .addMethod(generateUpdate(entityType, tableName, fields, primaryKey)) // 新增 update 方法
                .addMethod(generateDeleteById(tableName, primaryKey)) // 新增 deleteById 方法
                .build();

        try {
            JavaFile.builder(packageName, dao)
                    .build()
                    .writeTo(processingEnv.getFiler());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    private String getTableName(TypeElement entity) {
        Entity entityAnnotation = entity.getAnnotation(Entity.class);
        String tableName = entityAnnotation.tableName();
        if (!tableName.isEmpty()) {
            return tableName;
        }

        String className = entity.getSimpleName().toString();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < className.length(); i++) {
            char c = className.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private MethodSpec generateInsert(TypeElement entity, String tableName, List<FieldInfo> fields) {
        List<String> columns = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        List<CodeBlock> params = new ArrayList<>();
        for (FieldInfo field : fields) {
            if (field.isId && field.isAutoIncrement) continue; // 跳过自增主键
            columns.add(convertToSnakeCase(field.columnName)); // 调用转换方法
            placeholders.add("?");
            String getter = "get" + capitalize(field.field.getSimpleName().toString());
            params.add(CodeBlock.of("entity.$L()", getter));
        }

        MethodSpec.Builder method = MethodSpec.methodBuilder("insert")
                .addModifiers(Modifier.PUBLIC)
                .returns(long.class)
                .addParameter(ClassName.get(entity), "entity")
                .addStatement("String sql = $S", "INSERT INTO " + tableName + " (" + String.join(", ", columns) + ") VALUES (" + String.join(", ", placeholders) + ")")
                .addStatement("return executeUpdate(sql, $L)", CodeBlock.join(params, ", "));
        return method.build();
    }

    private MethodSpec generateFindById(TypeElement entity, String tableName, VariableElement primaryKey, List<FieldInfo> fields) {
        String columnName = fields.stream()
                .filter(f -> f.isId)
                .findFirst()
                .map(f -> convertToSnakeCase(f.columnName)) // 调用转换方法
                .orElse("id");

        CodeBlock.Builder mapper = CodeBlock.builder()
                .add("$T entity = new $T();\n", ClassName.get(entity), ClassName.get(entity));
        for (FieldInfo field : fields) {
            String setter = "set" + capitalize(field.field.getSimpleName().toString());
            String rsMethod = getResultSetMethod(field.field.asType());
            mapper.add("entity.$L(rs.$L($S));\n", setter, rsMethod, convertToSnakeCase(field.columnName));
        }
        mapper.add("return entity;");

        return MethodSpec.methodBuilder("findById")
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(entity))
                .addParameter(ClassName.get(primaryKey.asType()), "id")
                .addStatement("String sql = $S", "SELECT * FROM " + tableName + " WHERE " + columnName + " = ?")
                .addStatement("return executeQuery(sql, rs -> {\n" + mapper.build() + "\n}, id)")
                .build();
    }

    // 新增：将字段名转换为蛇形命名格式
    private String convertToSnakeCase(String fieldName) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // 辅助方法
    private String capitalize(String s) { return s.substring(0, 1).toUpperCase() + s.substring(1); }
    private String getResultSetMethod(TypeMirror type) {
        String typeName = type.toString();
        switch (typeName) {
            // 基本类型和包装类型
            case "int":     return "getInt";
            case "java.lang.Integer": return "getInt";
            case "long":    return "getLong";
            case "java.lang.Long":    return "getLong";
            case "boolean": return "getBoolean";
            case "java.lang.Boolean": return "getBoolean";
            case "short":   return "getShort";
            case "java.lang.Short":   return "getShort";
            case "byte":    return "getByte";
            case "java.lang.Byte":    return "getByte";
            case "float":   return "getFloat";
            case "java.lang.Float":   return "getFloat";
            case "double":  return "getDouble";
            case "java.lang.Double": return "getDouble";

            // 字符串和时间类型
            case "java.lang.String":  return "getString";
            case "java.util.Date":
            case "java.sql.Timestamp": return "getTimestamp";
            case "java.sql.Date":    return "getDate";
            case "java.math.BigDecimal": return "getBigDecimal";

            // 其他类型默认用 getObject
            default:
                // 如果是枚举类型，按字符串处理
                if (type.getKind() == TypeKind.DECLARED) {
                    Element element = ((DeclaredType) type).asElement();
                    if (element.getKind() == ElementKind.ENUM) {
                        return "getString"; // 假设枚举以字符串形式存储
                    }
                }
                return "getObject"; // 未知类型或自定义类型
        }
    }

    // 新增：生成 update 方法
    private MethodSpec generateUpdate(TypeElement entity, String tableName, List<FieldInfo> fields, VariableElement primaryKey) {
        // 新增：获取主键字段名
        String pkFieldName = primaryKey.getSimpleName().toString();
        String pkColumnName = convertToSnakeCase(pkFieldName);

        // 新增：生成动态 SET 子句
        CodeBlock.Builder setClauseBuilder = CodeBlock.builder();
        List<CodeBlock> params = new ArrayList<>();

        for (FieldInfo field : fields) {
            if (field.isId) continue; // 跳过主键字段

            String fieldName = field.field.getSimpleName().toString();
            String getter = "get" + capitalize(fieldName);
            String columnName = convertToSnakeCase(field.columnName);

            // 判断是否忽略字段
            CodeBlock ignoreCheck = CodeBlock.of("mutation.$L().isIgnore()", getter);
            CodeBlock isNullCheck = CodeBlock.of("mutation.$L().isNull()", getter);
            CodeBlock valueCheck = CodeBlock.of("mutation.$L().getValue()", getter);

            // 添加到 SET 子句
            setClauseBuilder.beginControlFlow("if (!mutation.$L().isIgnore())", getter)
                .beginControlFlow("if (!mutation.$L().isNull())", getter)
                .addStatement("setClause.append($S)", columnName + " = ?")
                .addStatement("params.add(mutation.$L().getValue())", getter)
                .endControlFlow()
                .beginControlFlow("else")
                .addStatement("setClause.append($S)", columnName + " = NULL")
                .endControlFlow()
                .endControlFlow();
        }

        // 新增：添加主键参数
        CodeBlock pkParam = CodeBlock.of("mutation.$L().getValue()", "get" + capitalize(pkFieldName));
        params.add(pkParam);

        ClassName arrayListClass = ClassName.get("java.util", "ArrayList");
//        ClassName listClass = ClassName.get("java.util", "List");
        // 构建完整方法
        MethodSpec.Builder method = MethodSpec.methodBuilder("update")
            .addModifiers(Modifier.PUBLIC)
            .returns(long.class)
            .addParameter(ClassName.get(processingEnv.getElementUtils().getPackageOf(entity).toString(), entity.getSimpleName() + "Mutation"), "mutation")
            .addStatement("StringBuilder setClause = new StringBuilder()")
            .addStatement("$L<Object> params = new $L<>()", arrayListClass, arrayListClass)
            .addCode(setClauseBuilder.build())
            .addStatement("String sql = $S + setClause.toString() + $S", "UPDATE " + tableName + " SET ", " WHERE " + pkColumnName + " = ?")
            .addStatement("return executeUpdate(sql, params.toArray())");

        return method.build();
    }

    // 新增：生成 deleteById 方法
    private MethodSpec generateDeleteById(String tableName, VariableElement primaryKey) {
        MethodSpec.Builder method = MethodSpec.methodBuilder("deleteById")
                .addModifiers(Modifier.PUBLIC)
                .returns(long.class)
                .addParameter(ClassName.get(primaryKey.asType()), "id")
                .addStatement("String sql = $S", "DELETE FROM " + tableName + " WHERE " + convertToSnakeCase(primaryKey.getSimpleName().toString()) + " = ?")
                .addStatement("return executeUpdate(sql, id)");
        return method.build();
    }

    // 新增：将原始类型转换为封装类型的方法
    private TypeName boxIfPrimitive(TypeName type) {
        boolean isInstanceOfClassName = type instanceof ClassName;
        if (!isInstanceOfClassName) {
//            ClassName className = (ClassName) type;
            String classNameString = type.toString();
            if (classNameString.equals("int")) {
                return ClassName.get(Integer.class);
            } else if (classNameString.equals("long")) {
                return ClassName.get(Long.class);
            } else if (classNameString.equals("boolean")) {
                return ClassName.get(Boolean.class);
            } else if (classNameString.equals("short")) {
                return ClassName.get(Short.class);
            } else if (classNameString.equals("byte")) {
                return ClassName.get(Byte.class);
            } else if (classNameString.equals("float")) {
                return ClassName.get(Float.class);
            } else if (classNameString.equals("double")) {
                return ClassName.get(Double.class);
            } else if (classNameString.startsWith("java.lang.")) {
                return type;
            } else {
                throw new RuntimeException("instance of ClassName:" + type);
            }
        }
//        throw new RuntimeException("not instance of ClassName:" + type);
        return type;
    }

    private void generateMutation(TypeElement entityType) {
        String packageName = processingEnv.getElementUtils().getPackageOf(entityType).toString();
        String entityName = entityType.getSimpleName().toString();
        String mutationName = entityName + "Mutation";

        // 收集字段信息
        List<FieldInfo> fields = new ArrayList<>();
        for (Element enclosed : entityType.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosed;
                fields.add(new FieldInfo(field, field.getSimpleName().toString(), false, false));
            }
        }


        // 使用 JavaPoet 生成 Mutation 类
        TypeSpec.Builder mutation = TypeSpec.classBuilder(mutationName)
                .addModifiers(Modifier.PUBLIC);

        // 为每个字段生成 UpdateField<T> 类型的字段
        for (FieldInfo field : fields) {
            TypeName fieldType = ParameterizedTypeName.get(
                    ClassName.get("com.lvonce.taitan", "UpdateField"),
                    boxIfPrimitive(TypeName.get(field.field.asType())) // 确保调用 boxIfPrimitive 方法处理原始类型
            );
            mutation.addField(fieldType, field.field.getSimpleName().toString(), Modifier.PRIVATE);
        }

//        throw new RuntimeException("generateMutation2");
//
        // 生成无参构造函数
        mutation.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .build());

        // 为每个字段生成 getter 和 setter 方法
        for (FieldInfo field : fields) {
            String fieldName = field.field.getSimpleName().toString();
            TypeName fieldType = ParameterizedTypeName.get(
                    ClassName.get("com.lvonce.taitan", "UpdateField"),
                    boxIfPrimitive(TypeName.get(field.field.asType())) // 确保调用 boxIfPrimitive 方法处理原始类型
            );

            // Getter 方法
            mutation.addMethod(MethodSpec.methodBuilder("get" + capitalize(fieldName))
                    .addModifiers(Modifier.PUBLIC)
                    .returns(fieldType)
                    .addStatement("return this.$L", fieldName)
                    .build());

            // Setter 方法
            mutation.addMethod(MethodSpec.methodBuilder("set" + capitalize(fieldName))
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(fieldType, fieldName)
                    .addStatement("this.$L = $L", fieldName, fieldName)
                    .build());
        }

        try {
            TypeSpec mutationTypeSpec = mutation.build();
            JavaFile.builder(packageName, mutationTypeSpec)
                    .build()
                    .writeTo(processingEnv.getFiler());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    private void generateFieldExprs(TypeElement entityType) {
        String packageName = processingEnv.getElementUtils().getPackageOf(entityType).toString();
        String entityName = entityType.getSimpleName().toString();
        String exprsName = entityName + "Exprs";


        // 创建 sealed interface
        TypeSpec.Builder exprsInterfaceBuilder = TypeSpec.interfaceBuilder(exprsName)
                .addModifiers(Modifier.PUBLIC, Modifier.SEALED)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "ALL")
                        .build())
                .addJavadoc("Generated by OrmProcessor. Do not modify.\n")
                .addSuperinterface(ClassName.get("com.lvonce.taitan.logic", "FieldExpr"));
//                .addPermittedSubclasses(permitsList.stream().toList())
//                .build();

        // 收集字段并构建 records
        List<TypeSpec> recordList = new ArrayList<>();


        for (Element enclosed : entityType.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosed;
                String fieldName = capitalize(field.getSimpleName().toString());
                TypeName fieldType = boxIfPrimitive(TypeName.get(field.asType()));

                // 创建 record: ID(Expr<Long>) implements UserEntityExprs
                TypeSpec record = TypeSpec.recordBuilder(fieldName)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addSuperinterface(ClassName.get(packageName, exprsName))
                        .recordConstructor(MethodSpec.constructorBuilder()
                                .addParameter(ParameterizedTypeName.get(
                                        ClassName.get("com.lvonce.taitan.logic", "Expr"),
                                        fieldType
                                ), "expr")
                                .build())
                        .addMethod(MethodSpec.constructorBuilder()
                                .addModifiers(Modifier.PUBLIC)
                                .addParameter(ClassName.get("com.lvonce.taitan.logic", "Cmp"), "cmp")
                                .addParameter(fieldType, "value")
                                .addStatement("this($T.of(cmp, value))", ClassName.get("com.lvonce.taitan.logic", "Expr"))
                                .build())
                        .addMethod(MethodSpec.methodBuilder("name")
                                .addAnnotation(Override.class)
                                .addModifiers(Modifier.PUBLIC)
                                .returns(String.class)
                                .addStatement("return $S", fieldName)
                                .build())
                        .addMethod(MethodSpec.methodBuilder("eq")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .addParameter(fieldType, "value")
                                .returns(ClassName.get("", fieldName))
                                .addStatement("return new $L($T.EQ, value)", fieldName, ClassName.get("com.lvonce.taitan.logic", "Cmp"))
                                .build())
                        .addMethod(MethodSpec.methodBuilder("isNull")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .returns(ClassName.get("", fieldName))
                                .addStatement("return new $L($T.IS_NULL, null)", fieldName, ClassName.get("com.lvonce.taitan.logic", "Cmp"))
                                .build())
                        .build();

//                recordList.add(record);
                exprsInterfaceBuilder.addType(record);
                exprsInterfaceBuilder.addPermittedSubclass(ClassName.get(packageName,  exprsName).nestedClass(record.name()));


            }
        }



        // 写入文件
        try {
            JavaFile.builder(packageName, exprsInterfaceBuilder.build())
                    .addFileComment("Generated by OrmProcessor. Do not modify.")
                    .build()
                    .writeTo(processingEnv.getFiler());

//            for (TypeSpec record : recordList) {
//                JavaFile.builder(packageName, record)
//                        .addFileComment("Generated by OrmProcessor. Do not modify.")
//                        .build()
//                        .writeTo(processingEnv.getFiler());
//            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static class FieldInfo {
        VariableElement field;
        String columnName;
        boolean isId;
        boolean isAutoIncrement;
        // 构造方法等
        public FieldInfo(VariableElement field, String columnName, boolean isId, boolean isAutoIncrement) {
            this.field = field;
            this.columnName = columnName;
            this.isId = isId;
            this.isAutoIncrement = isAutoIncrement;
        }
    }
}
