package com.lvonce.taitan.logic;

import com.lvonce.taitan.SqlFragment;

import java.util.ArrayList;
import java.util.List;

public record Or(List<FieldExpr> exprs) implements FieldExpr {
    public Or(FieldExpr... exprs) {
        this(List.of(exprs));
    }

    @Override
    public String name() {
        throw new UnsupportedOperationException("And expressions has no name");
    }

    @Override
    public Expr<?> expr() {
        throw new UnsupportedOperationException("Or expressions don't have a single expr");
    }

    @Override
    public SqlFragment toSql() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");

        List<Object> params = new ArrayList<>();

        for (int i = 0; i < exprs.size(); i++) {
            if (i > 0) sb.append(" OR ");
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
