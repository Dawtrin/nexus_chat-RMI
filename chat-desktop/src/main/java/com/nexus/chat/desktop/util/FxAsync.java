package com.nexus.chat.desktop.util;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Utility for safely dispatching blocking RMI calls off the JavaFX Application Thread
 * and applying UI updates on Platform.runLater().
 */
public final class FxAsync {
    private static final Logger log = LoggerFactory.getLogger(FxAsync.class);

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "nexus-fx-worker");
        t.setDaemon(true);
        return t;
    });

    private FxAsync() {
    }

    public static void runAsync(Runnable backgroundTask) {
        EXECUTOR.submit(() -> {
            try {
                backgroundTask.run();
            } catch (Exception e) {
                log.error("Unhandled error in async background task: {}", e.getMessage(), e);
            }
        });
    }

    public static <T> void execute(Callable<T> backgroundTask,
                                   Consumer<T> onSuccess,
                                   Consumer<Throwable> onError) {
        EXECUTOR.submit(() -> {
            try {
                T result = backgroundTask.call();
                if (onSuccess != null) {
                    Platform.runLater(() -> onSuccess.accept(result));
                }
            } catch (Throwable t) {
                log.warn("Async execution failure: {}", t.getMessage());
                if (onError != null) {
                    Platform.runLater(() -> onError.accept(t));
                }
            }
        });
    }

    public static void shutdown() {
        EXECUTOR.shutdownNow();
    }
}
