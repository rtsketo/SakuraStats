package eu.rtsketo.sakurastats.control;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadPool {
    private ThreadPool() {}
    private static ThreadPoolExecutor fixedPool;
    private static ThreadPoolExecutor cachePool;

    public static ThreadPoolExecutor getFixedPool() {
        if (fixedPool == null) fixedPool = (ThreadPoolExecutor)
                Executors.newFixedThreadPool( 12);
        return fixedPool;
    }

    public static ThreadPoolExecutor getCachePool() {
        if (cachePool == null) cachePool = (ThreadPoolExecutor)
                Executors.newCachedThreadPool();
        return cachePool;
    }
}
