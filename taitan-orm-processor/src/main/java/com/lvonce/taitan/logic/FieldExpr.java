package com.lvonce.taitan.logic;

import com.lvonce.taitan.SqlFragment;

public interface FieldExpr<T> {
    String name();
    Expr<T> expr();
    default Cmp cmp() {
        return expr().cmp();
    }
    default T value() {
        return expr().val();
    }
    default SqlFragment toSql() {
        String field = name();
        SqlFragment fragment = expr().toSql();

        if (!fragment.hasParam()) {
            return new SqlFragment(field + " " + fragment.sql(), false, null);
        } else {
            return new SqlFragment(field + " " + fragment.sql(), true, fragment.params());
        }
    }
}
