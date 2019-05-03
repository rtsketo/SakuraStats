package eu.rtsketo.sakurastats.control;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static java.lang.Math.max;
import static java.lang.Math.min;


public class ThreadPool {
    private ThreadPool() {}
    private static ThreadPoolExecutor fixedPool;
    private static ThreadPoolExecutor cachePool;

    public static synchronized ThreadPoolExecutor getFixedPool() {
        if (fixedPool == null) {
            int threads = Runtime.getRuntime().availableProcessors();
            int maxThreads = min(max(4,threads*2),20);
            fixedPool = (ThreadPoolExecutor)
                    Executors.newFixedThreadPool(maxThreads);
        }

        return fixedPool;
    }

    public static synchronized ThreadPoolExecutor getCachePool() {
        if (cachePool == null) cachePool = (ThreadPoolExecutor)
                Executors.newCachedThreadPool();
        return cachePool;
    }
}
