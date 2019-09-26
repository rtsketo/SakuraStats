package eu.rtsketo.sakurastats.control;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


public class ThreadPool {
    private ThreadPool() {}
    private static ThreadPoolExecutor fixedPool;
    private static ThreadPoolExecutor cachePool;

    public static synchronized ThreadPoolExecutor getFixedPool() {
        if (fixedPool == null) {
//            int threads = Runtime.getRuntime().availableProcessors();
            int maxThreads = 20;
//                    min(max(10,threads*2),25);
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
