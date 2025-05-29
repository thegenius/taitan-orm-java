package com.lvonce.taitan.logic;

import com.lvonce.taitan.SqlFragment;
import com.lvonce.taitan.logic.Expr;

import java.util.ArrayList;
import java.util.List;

public record And(List<FieldExpr> exprs) implements FieldExpr {
    public And(FieldExpr... exprs) {
        this(List.of(exprs));
    }

    @Override
    public String name() {
        throw new UnsupportedOperationException("And expressions has no name");
    }

    @Override
    public Expr<?> expr() {
        throw new UnsupportedOperationException("And expressions don't have a single expr");

    }

    public SqlFragment toSql() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");

        List<Object> params = new ArrayList<>();

        for (int i = 0; i < exprs.size(); i++) {
            if (i > 0) sb.append(" AND ");
            SqlFragment fragment = exprs.get(i).toSql();
            sb.append(fragment.sql());
            if (fragment.hasParam()) {
                params.add(fragment.param());
            }
        }

        sb.append(")");
        return new SqlFragment(sb.toString(), !params.isEmpty(), params.toArray());
    }


}
