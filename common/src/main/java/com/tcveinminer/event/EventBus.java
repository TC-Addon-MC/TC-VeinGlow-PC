package com.tcveinminer.event;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * Simple, fast, thread-safe event bus.
 * <p>
 * Replaces Fabric's {@code EventFactory.createArrayBacked()} in the common module,
 * eliminating the Fabric API dependency from shared code.
 * <p>
 * Design notes:
 * <ul>
 *   <li>{@link CopyOnWriteArrayList} provides lock-free reads (fast game loop) at
 *       the cost of slightly slower registration (acceptable — registration is rare).</li>
 *   <li>The {@code invoker} field is re-built on every {@link #register}/{@link #unregister}
 *       call, so readers pay zero cost per invocation.</li>
 *   <li>Thread-safe: multiple threads can call {@link #invoker()} concurrently.</li>
 * </ul>
 *
 * <h3>Usage example</h3>
 * <pre>{@code
 * EventBus<SessionStartCallback> bus = EventBus.create(listeners -> event -> {
 *     for (var l : listeners) {
 *         EventResult r = l.onStart(event);
 *         if (r == EventResult.DENY) return r;
 *     }
 *     return EventResult.PASS;
 * });
 *
 * bus.register(myListener);
 * bus.invoker().onStart(event);
 * }</pre>
 *
 * @param <T> The functional listener interface type.
 */
public final class EventBus<T> {

    private final List<T> listeners = new CopyOnWriteArrayList<>();
    private final Function<List<T>, T> invokerFactory;
    private volatile T invoker;

    private EventBus(Function<List<T>, T> invokerFactory) {
        this.invokerFactory = invokerFactory;
        this.invoker = invokerFactory.apply(Collections.unmodifiableList(listeners));
    }

    /**
     * Create a new event bus with the given invoker factory.
     *
     * @param invokerFactory A function that, given the current list of listeners,
     *                       returns a composed invoker that calls all of them.
     */
    public static <T> EventBus<T> create(Function<List<T>, T> invokerFactory) {
        return new EventBus<>(invokerFactory);
    }

    /**
     * Register a listener. Rebuilds the invoker immediately.
     * Safe to call from any thread.
     */
    public void register(T listener) {
        listeners.add(listener);
        invoker = invokerFactory.apply(Collections.unmodifiableList(listeners));
    }

    /**
     * Unregister a listener. Rebuilds the invoker immediately.
     * Safe to call from any thread.
     */
    public void unregister(T listener) {
        listeners.remove(listener);
        invoker = invokerFactory.apply(Collections.unmodifiableList(listeners));
    }

    /**
     * @return the current composed invoker — call its method to fire the event.
     */
    public T invoker() {
        return invoker;
    }

    /** @return current number of registered listeners. */
    public int listenerCount() {
        return listeners.size();
    }
}
