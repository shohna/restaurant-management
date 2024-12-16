package application;

import java.util.concurrent.*;

public class RMSMultithreading {
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    // Submit a task for asynchronous processing
    public static <T> CompletableFuture<T> submitTask(Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executorService);
    }

    // Shut down the ExecutorService (e.g., when the application exits)
    public static void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}
