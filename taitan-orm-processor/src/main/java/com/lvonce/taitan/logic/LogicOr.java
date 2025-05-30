package com.lvonce.taitan.logic;

import com.lvonce.taitan.SqlFragment;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("rawtypes")
public interface LogicOr extends FieldExpr {
//    public LogicOr(FieldExpr... exprs) {
//        this(List.of(exprs));
//    }
    FieldExpr<?>[] fieldExprs();

    default String name() {
        throw new UnsupportedOperationException("And expressions has no name");
    }

    default Expr<?> expr() {
        throw new UnsupportedOperationException("Or expressions don't have a single expr");
    }


    default SqlFragment toSql() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");

        List<Object> params = new ArrayList<>();
        FieldExpr<?>[] exprArray = fieldExprs();
        for (int i = 0; i < exprArray.length; i++) {
            if (i > 0) sb.append(" OR ");
            SqlFragment fragment = exprArray[i].toSql();
            sb.append(fragment.sql());
            if (fragment.hasParam()) {
                params.addAll(fragment.params());
            }
        }

        sb.append(")");
        return new SqlFragment(sb.toString(), !params.isEmpty(), params);
    }
}
