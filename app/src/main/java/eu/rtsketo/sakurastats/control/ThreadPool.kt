package eu.rtsketo.sakurastats.control

import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor

object ThreadPool {
    //            int threads = Runtime.getRuntime().availableProcessors();
    //                    min(max(10,threads*2),25);
    @get:Synchronized
    var fixedPool: ThreadPoolExecutor? = null
        get() {
            if (field == null) { //            int threads = Runtime.getRuntime().availableProcessors();
                val maxThreads = 20
                //                    min(max(10,threads*2),25);
                field = Executors.newFixedThreadPool(maxThreads) as ThreadPoolExecutor
            }
            return field
        }
        private set
    @get:Synchronized
    var cachePool: ThreadPoolExecutor? = null
        get() {
            if (field == null) field = Executors.newCachedThreadPool() as ThreadPoolExecutor
            return field
        }
        private set

}