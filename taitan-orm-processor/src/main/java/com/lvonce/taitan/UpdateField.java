package com.lvonce.taitan;

public class UpdateField<T> {
    public enum State { IGNORE, SET_NULL, SET_VALUE }

    private final State state;
    private final T value;

    private UpdateField(State state, T value) {
        this.state = state;
        this.value = value;
    }

    public static <T> UpdateField<T> ignore() {
        return new UpdateField<>(State.IGNORE, null);
    }

    public static <T> UpdateField<T> setNull() {
        return new UpdateField<>(State.SET_NULL, null);
    }

    public static <T> UpdateField<T> setValue(T value) {
        return new UpdateField<>(State.SET_VALUE, value);
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

    public boolean isNull() {
        return state == State.SET_NULL;
    }

    public T getValue() {
        return value;
    }
}