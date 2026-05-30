package com.tcveinminer.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class Event<T> {

    private final List<T> listeners = new ArrayList<>();
    private T invoker;
    private final Function<T[], T> invokerFactory;

    @SuppressWarnings("unchecked")
    private Event(Class<T> type, Function<T[], T> invokerFactory) {
        this.invokerFactory = invokerFactory;
        this.invoker = invokerFactory.apply((T[]) java.lang.reflect.Array.newInstance(type, 0));
    }

    @SuppressWarnings("unchecked")
    public static <T> Event<T> createArrayBacked(Class<T> type, Function<T[], T> invokerFactory) {
        return new Event<>(type, invokerFactory);
    }

    @SuppressWarnings("unchecked")
    public void register(T listener) {
        listeners.add(listener);
        invoker = invokerFactory.apply(listeners.toArray(
            (T[]) java.lang.reflect.Array.newInstance(listener.getClass().getInterfaces()[0], 0)
        ));
    }

    public T invoker() {
        return invoker;
    }
}
