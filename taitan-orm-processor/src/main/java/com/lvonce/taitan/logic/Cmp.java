package com.lvonce.taitan.logic;

public enum Cmp {
    EQ,
    NE,
    GT,
    GE,
    LT,
    LE,
    LIKE,
    IN,
    BETWEEN,
    IS_NULL,
    IS_NOT_NULL;
    public String getSql() {
        return switch (this) {
            case EQ -> "=";
            case NE -> "!=";
            case GT -> ">";
            case GE -> ">=";
            case LT -> "<";
            case LE -> "<=";
            case LIKE -> "LIKE";
            case IN -> "IN";
            case BETWEEN -> "BETWEEN";
            case IS_NULL -> "IS NULL";
            case IS_NOT_NULL -> "IS NOT NULL";
        };
    }
}
