package com.lvonce.taitan.logic;


import com.lvonce.taitan.SqlFragment;

@SuppressWarnings("rawtypes")
public interface LogicNot extends FieldExpr {
    default String name() {
        throw new UnsupportedOperationException("Not expressions has no name");
    }
    default Expr<?> expr() {
        throw new UnsupportedOperationException("And expressions don't have a single expr");
    }

    FieldExpr<?> fieldExpr();

    default SqlFragment toSql() {
        SqlFragment fragment = fieldExpr().toSql();
        String sb = "NOT(" + fragment.sql() + ")";
        return new SqlFragment(sb, fragment.hasParam(), fragment.params());
    }
}
