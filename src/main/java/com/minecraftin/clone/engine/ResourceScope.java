package com.minecraftin.clone.engine;

import java.util.ArrayDeque;

// 依建立順序的反向釋放資源；部分初始化失敗時也會執行所有清理，並保留原始例外。
public final class ResourceScope implements AutoCloseable {
    private final ArrayDeque<AutoCloseable> resources = new ArrayDeque<>();

    public <T extends AutoCloseable> T add(T resource) {
        resources.push(resource);
        return resource;
    }

    // 建構完成後把所有權交給呼叫端；未呼叫時由 try-with-resources 回收。
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
