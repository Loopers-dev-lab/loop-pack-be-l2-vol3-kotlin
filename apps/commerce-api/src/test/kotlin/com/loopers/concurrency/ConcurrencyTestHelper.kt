package com.loopers.concurrency

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object ConcurrencyTestHelper {

    private const val TIMEOUT_SECONDS = 30L

    fun <T> executeConcurrently(actions: List<() -> T>): List<Result<T>> {
        val threadCount = actions.size
        val executorService = Executors.newFixedThreadPool(threadCount)
        val readyLatch = CountDownLatch(threadCount)
        val startLatch = CountDownLatch(1)

        try {
            val futures = actions.map { action ->
                executorService.submit<Result<T>> {
                    readyLatch.countDown()
                    startLatch.await()
                    runCatching { action() }
                }
            }

            readyLatch.await()
            startLatch.countDown()

            return futures.map { it.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }
        } finally {
            executorService.shutdown()
            require(executorService.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                "스레드 풀이 $TIMEOUT_SECONDS 초 내에 종료되지 않았습니다."
            }
        }
    }
}
