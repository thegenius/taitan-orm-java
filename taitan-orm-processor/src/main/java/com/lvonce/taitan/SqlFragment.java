package com.lvonce.taitan;

import java.util.List;

public record SqlFragment(
        String sql,
        boolean hasParam,
        List<Object> params
) {
}
