package com.lvonce.taitan;

public record SqlFragment(
        String sql,
        boolean hasParam,
        Object param
) {
}
