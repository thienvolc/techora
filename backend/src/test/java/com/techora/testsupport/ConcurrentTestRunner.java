package com.techora.testsupport;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

public final class ConcurrentTestRunner {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private ConcurrentTestRunner() {
    }

    public static <T> List<T> run(int concurrentTasks, Callable<T> task) throws Exception {
        return run(concurrentTasks, ignored -> task);
    }

    public static <T> List<T> run(int concurrentTasks,
                                  IntFunction<Callable<T>> taskFactory) throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(concurrentTasks);
        CountDownLatch ready = new CountDownLatch(concurrentTasks);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<T>> futures = IntStream.range(0, concurrentTasks)
                    .mapToObj(index -> executor.submit(synchronizedTask(taskFactory.apply(index), ready, start)))
                    .toList();

            if (!ready.await(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("Concurrent tasks were not ready");
            }

            start.countDown();

            return futures.stream()
                    .map(ConcurrentTestRunner::getResult)
                    .toList();
        } finally {
            executor.shutdownNow();
        }
    }

    private static <T> Callable<T> synchronizedTask(Callable<T> task,
                                                    CountDownLatch ready,
                                                    CountDownLatch start) {
        return () -> {
            ready.countDown();
            if (!start.await(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("Concurrent tasks did not start");
            }
            return task.call();
        };
    }

    private static <T> T getResult(Future<T> future) {
        try {
            return future.get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException ex) {
            throw propagate(ex.getCause());
        } catch (Exception ex) {
            throw new IllegalStateException("Concurrent task failed", ex);
        }
    }

    private static RuntimeException propagate(Throwable cause) {
        if (cause instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        if (cause instanceof Error error) {
            throw error;
        }
        return new IllegalStateException("Concurrent task failed", cause);
    }
}
