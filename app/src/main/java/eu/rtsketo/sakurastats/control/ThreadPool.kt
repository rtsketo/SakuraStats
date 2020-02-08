package eu.rtsketo.sakurastats.control

import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor

object ThreadPool {
    val fixedPool = Executors.newFixedThreadPool(10) as ThreadPoolExecutor
    var cachePool = Executors.newCachedThreadPool() as ThreadPoolExecutor
}