package org.acme;

import com.lvonce.taitan.logic.Cmp;
import com.lvonce.taitan.logic.Expr;
import com.lvonce.taitan.logic.FieldExpr;

public sealed interface UserEntityExprs2 extends FieldExpr permits UserEntityExprs2.ID, UserEntityExprs2.Age {


    record ID(Expr<Long> expr) implements UserEntityExprs2 {
        public ID(Cmp cmp, Long value) {
            this(Expr.of(cmp, value));
        }
        @Override
        public String name() {
            return "id";
        }
    }
    record Age(Expr<Integer> expr) implements UserEntityExprs2 {
        public Age(Cmp cmp, Integer value) {
            this(Expr.of(cmp, value));
        }

        @Override
        public String name() {
            return "age";
        }
    }
}
