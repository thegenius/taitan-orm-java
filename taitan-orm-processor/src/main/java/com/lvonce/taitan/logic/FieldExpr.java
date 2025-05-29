package com.lvonce.taitan.logic;

import com.lvonce.taitan.SqlFragment;

public interface FieldExpr {
    String name();
    Expr<?> expr();
    default Cmp cmp() {
        return expr().cmp();
    }
    default Object value() {
        return expr().val();
    }
    default SqlFragment toSql() {
        String field = name();
        SqlFragment fragment = expr().toSql();

        if (!fragment.hasParam()) {
            return new SqlFragment(field + " " + fragment.sql(), false, null);
        } else {
            return new SqlFragment(field + " " + fragment.sql(), true, fragment.param());
        }
    }
}
