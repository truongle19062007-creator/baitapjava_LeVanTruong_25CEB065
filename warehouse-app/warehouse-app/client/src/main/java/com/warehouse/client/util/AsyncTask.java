package com.warehouse.client.util;

import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.function.Consumer;

/**
 * Tiện ích chạy 1 lời gọi API (blocking, có thể chậm vì qua mạng) trên thread riêng,
 * tránh đứng giao diện JavaFX. Kết quả/lỗi được đưa trở lại JavaFX Application Thread
 * thông qua Platform.runLater, vì mọi thay đổi lên UI component PHẢI thực hiện trên
 * JavaFX Application Thread.
 */
public final class AsyncTask {

    private AsyncTask() {
    }

    public static <T> void run(java.util.concurrent.Callable<T> backgroundWork,
                                Consumer<T> onSuccess,
                                Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return backgroundWork.call();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException()));
        Thread thread = new Thread(task, "api-worker");
        thread.setDaemon(true);
        thread.start();
    }

    /** Biến thể cho các action không cần trả về dữ liệu (void). */
    public static void runVoid(VoidCallable backgroundWork,
                                Runnable onSuccess,
                                Consumer<Throwable> onError) {
        run(() -> {
            backgroundWork.call();
            return null;
        }, ignored -> onSuccess.run(), onError);
    }

    @FunctionalInterface
    public interface VoidCallable {
        void call() throws Exception;
    }

    public static void runOnUiThread(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }
}
