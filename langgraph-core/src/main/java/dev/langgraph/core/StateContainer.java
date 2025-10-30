package dev.langgraph.core;

import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class StateContainer {
    
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile State currentState;

    public StateContainer() {
        this(State.empty());
    }

    public StateContainer(State initialState) {
        this.currentState = initialState != null ? initialState : State.empty();
    }

    public State get() {
        lock.readLock().lock();
        try {
            return currentState;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void set(State state) {
        lock.writeLock().lock();
        try {
            this.currentState = state != null ? state : State.empty();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public State update(String key, Object value) {
        lock.writeLock().lock();
        try {
            currentState = currentState.with(key, value);
            return currentState;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public State remove(String key) {
        lock.writeLock().lock();
        try {
            currentState = currentState.without(key);
            return currentState;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public <T> Optional<T> getValue(String key) {
        lock.readLock().lock();
        try {
            return currentState.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean hasKey(String key) {
        lock.readLock().lock();
        try {
            return currentState.has(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            currentState = State.empty();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String toString() {
        return "StateContainer{state=" + get() + "}";
    }
}
