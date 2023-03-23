package tourGuide.service;

import org.springframework.beans.factory.DisposableBean;

import java.util.concurrent.*;

public class TaskExecutorService implements DisposableBean {

    public static final int CORE_NUMBERS = Runtime.getRuntime().availableProcessors();
    private final ThreadPoolExecutor executor;

    public TaskExecutorService(int waitTime, int serviceTime) {
        System.out.println("CORE NB: " + CORE_NUMBERS);
        executor = new ThreadPoolExecutor(CORE_NUMBERS * (1 + waitTime/serviceTime), Integer.MAX_VALUE, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        System.out.println("FINAL POOL SIZE: " + executor.getCorePoolSize());
    }

    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

    public Future<?> submit(Runnable task) {
        return executor.submit(task);
    }

    @Override
    public void destroy() throws Exception {
        executor.shutdownNow();
    }
}
