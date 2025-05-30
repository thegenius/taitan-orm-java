package org.acme;

import com.lvonce.taitan.logic.*;

public sealed interface UserEntityExprs2 extends FieldExpr permits UserEntityExprs2.And,  UserEntityExprs2.Not, UserEntityExprs2.ID, UserEntityExprs2.Age {

    record And(FieldExpr<?>[] exprs) implements UserEntityExprs2, LogicAnd {

        public And(UserEntityExprs2... exprs) {
             this((FieldExpr<?>[]) exprs);
        }

        @Override
        public FieldExpr<?>[] fieldExprs() {
            return exprs;
        }
    }

    record Not(FieldExpr<?> inner_expr) implements UserEntityExprs2, LogicNot {

        public Not(UserEntityExprs2 expr) {
            this((FieldExpr<?>) expr);
        }

        @Override
        public FieldExpr<?> fieldExpr() {
            return inner_expr;
        }
    }

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
