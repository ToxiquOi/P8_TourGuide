package tourGuide.service;

import org.springframework.beans.factory.DisposableBean;

import java.util.concurrent.*;

public class TaskExecutorService implements DisposableBean {

    private final ThreadPoolExecutor executor;

    public TaskExecutorService() {
        executor = new ThreadPoolExecutor(5000, Integer.MAX_VALUE, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    public TaskExecutorService(int corePoolSize) {
        executor = new ThreadPoolExecutor(corePoolSize, Integer.MAX_VALUE, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
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
