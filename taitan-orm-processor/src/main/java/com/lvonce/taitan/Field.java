package com.lvonce.taitan;

public class Field<T> {
    public enum State {IGNORE, EMPTY, VALUED}

    private final State state;
    private final T value;

    private Field(State state, T value) {
        this.state = state;
        this.value = value;
    }

    public static <T> Field<T> ignore() {
        return new Field<>(State.IGNORE, null);
    }

    public static <T> Field<T> empty() {
        return new Field<>(State.EMPTY, null);
    }

    public static <T> Field<T> setValue(T value) {
        return new Field<>(State.VALUED, value);
    }

    public State getState() {
        return state;
    }

    public boolean isIgnore() {
        return state == State.IGNORE;
    }

    public boolean isNotIgnore() {
        return state != State.IGNORE;
    }

    public boolean isEmpty() {
        return state == State.EMPTY;
    }

    public boolean isNotNull() {
        return state != State.EMPTY;
    }

    public boolean isValued() {
        return state == State.VALUED;
    }

    public boolean isNotValued() {
        return state != State.VALUED;
    }

    public T getValue() {
        return value;
    }
}