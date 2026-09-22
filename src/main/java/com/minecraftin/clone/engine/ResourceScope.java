package com.minecraftin.clone.engine;

import java.util.ArrayDeque;

// Close registered resources in reverse order, even if cleanup fails.
// Report the first cleanup failure and retain later failures as suppressed exceptions.
public final class ResourceScope implements AutoCloseable {
    private final ArrayDeque<AutoCloseable> resources = new ArrayDeque<>();

    public <T extends AutoCloseable> T add(T resource) {
        resources.push(resource);
        return resource;
    }

    // Transfer ownership to the caller without closing resources; otherwise close() releases them.
    public void release() {
        resources.clear();
    }

    @Override
    public void close() {
        Throwable failure = null;
        while (!resources.isEmpty()) {
            try {
                resources.pop().close();
            } catch (Throwable e) {
                if (failure == null) {
                    failure = e;
                } else if (failure != e) {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure instanceof Error error) {
            throw error;
        }
        if (failure instanceof RuntimeException exception) {
            throw exception;
        }
        if (failure != null) {
            throw new IllegalStateException("Failed to close resources", failure);
        }
    }
}
