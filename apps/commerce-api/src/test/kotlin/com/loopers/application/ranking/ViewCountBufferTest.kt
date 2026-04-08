package com.loopers.application.ranking

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class ViewCountBufferTest {

    private val buffer = ViewCountBuffer()

    @DisplayName("VIEW 버퍼")
    @Nested
    inner class Buffer {

        @DisplayName("같은 상품의 조회수가 누적된다")
        @Test
        fun incrementAccumulates() {
            buffer.increment(101L)
            buffer.increment(101L)
            buffer.increment(101L)

            val result = buffer.drainAll()

            assertThat(result[101L]).isEqualTo(3)
        }

        @DisplayName("다른 상품은 독립적으로 누적된다")
        @Test
        fun independentProducts() {
            buffer.increment(101L)
            buffer.increment(202L)
            buffer.increment(202L)

            val result = buffer.drainAll()

            assertThat(result[101L]).isEqualTo(1)
            assertThat(result[202L]).isEqualTo(2)
        }

        @DisplayName("drain 후 버퍼가 비워진다")
        @Test
        fun drainClearsBuffer() {
            buffer.increment(101L)
            buffer.drainAll()

            val result = buffer.drainAll()

            assertThat(result).isEmpty()
        }

        @DisplayName("100개 스레드가 동시에 같은 상품을 increment해도 정확히 누적된다")
        @Test
        fun concurrentIncrement() {
            val threadCount = 100
            val executor = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)

            repeat(threadCount) {
                executor.submit {
                    try {
                        buffer.increment(101L)
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executor.shutdown()

            val result = buffer.drainAll()
            assertThat(result[101L]).isEqualTo(100)
        }
    }
}
