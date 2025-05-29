package com.lvonce.taitan.logic;


import com.lvonce.taitan.SqlFragment;

import java.util.ArrayList;
import java.util.List;

public record Not(FieldExpr inner_expr) implements FieldExpr {
    public Not(FieldExpr inner_expr) {
        this.inner_expr = inner_expr;
    }

    @Override
    public String name() {
        throw new UnsupportedOperationException("And expressions has no name");
    }

    @Override
    public Expr<?> expr() {
        return this.inner_expr.expr();
    }

    public SqlFragment toSql() {
        SqlFragment fragment = inner_expr.toSql();
        String sb = "NOT(" + fragment.sql() + ")";
        return new SqlFragment(sb, fragment.hasParam(), fragment.param());
    }
}
