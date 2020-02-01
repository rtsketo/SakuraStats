package eu.rtsketo.sakurastats.control

import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor

object ThreadPool {
    val fixedPool: ThreadPoolExecutor = Executors.newFixedThreadPool(20) as ThreadPoolExecutor
    var cachePool: ThreadPoolExecutor = Executors.newCachedThreadPool() as ThreadPoolExecutor
}