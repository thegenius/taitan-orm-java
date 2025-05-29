package com.lvonce.taitan.logic;


import com.lvonce.taitan.SqlFragment;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public record Expr<T>(
        Cmp cmp,
        T val
) {
    public static <T> Expr<T> of(Cmp cmp, T val)
    {
        if (cmp == Cmp.IS_NULL || cmp == Cmp.IS_NOT_NULL) {
            return new Expr<>(cmp, null);
        }
        return new Expr<>(cmp, val);
    }
    public SqlFragment toSql() {
        String sql = cmp.getSql();


        switch (cmp) {
            case IS_NULL, IS_NOT_NULL:
                sql = cmp.getSql(); // "IS NULL" 或 "IS NOT NULL"
                return new SqlFragment(sql, false, null);

            case IN:
                if (!(val instanceof Iterable)) {
                    throw new IllegalArgumentException("IN operator requires an Iterable value");
                }
                // 简化处理，生成 IN (?, ?, ?)
                String placeholders = StreamSupport.stream(((Iterable<?>) val).spliterator(), false)
                        .map(e -> "?")
                        .collect(Collectors.joining(", "));
                sql = "IN (" + placeholders + ")";
                return new SqlFragment(sql, true, val);

            case BETWEEN:
                // 假设 val 是一个包含两个元素的数组或自定义区间对象
                if (!(val instanceof Object[] arr) || arr.length != 2) {
                    throw new IllegalArgumentException("BETWEEN requires two values");
                }
                sql = "BETWEEN ? AND ?";
                return new SqlFragment(sql, true, val);

            default:
                // 普通操作符：=、>、LIKE 等
                sql += " ?";
                return new SqlFragment(sql, true, val);
        }
    }
}
