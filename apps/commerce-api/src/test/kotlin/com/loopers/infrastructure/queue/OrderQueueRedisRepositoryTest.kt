package com.loopers.infrastructure.queue

import com.loopers.domain.queue.OrderQueueRepository
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@SpringBootTest
class OrderQueueRedisRepositoryTest @Autowired constructor(
    private val orderQueueRepository: OrderQueueRepository,
    private val redisCleanUp: RedisCleanUp,
) {

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @Nested
    @DisplayName("enqueue")
    inner class Enqueue {

        @Test
        @DisplayName("같은 userId로 두 번 enqueue하면 false를 반환한다")
        fun `같은 userId로 두 번 enqueue하면 false를 반환한다`() {
            // given
            val userId = 1L
            val score = System.currentTimeMillis().toDouble()

            // when
            val first = orderQueueRepository.enqueue(userId, score)
            val second = orderQueueRepository.enqueue(userId, score + 1000)

            // then
            assertThat(first).isTrue()
            assertThat(second).isFalse()
        }

        @Test
        @DisplayName("같은 userId로 10개 스레드가 동시에 enqueue하면, 1번만 등록된다")
        fun `같은 userId로 10개 스레드가 동시에 enqueue하면, 1번만 등록된다`() {
            // given
            val userId = 1L
            val threadCount = 10
            val executor = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val results = mutableListOf<Boolean>()

            // when
            repeat(threadCount) {
                executor.submit {
                    try {
                        val result = orderQueueRepository.enqueue(userId, System.currentTimeMillis().toDouble())
                        synchronized(results) { results.add(result) }
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executor.shutdown()

            // then
            assertThat(results).hasSize(threadCount)
            assertThat(results.count { it }).isEqualTo(1)
            assertThat(orderQueueRepository.getTotalSize()).isEqualTo(1L)
        }
    }

    @Nested
    @DisplayName("getPosition")
    inner class GetPosition {

        @Test
        @DisplayName("enqueue 후 getPosition이 정확한 순번을 반환한다")
        fun `enqueue 후 getPosition이 정확한 순번을 반환한다`() {
            // given
            val baseScore = System.currentTimeMillis().toDouble()
            orderQueueRepository.enqueue(1L, baseScore)
            orderQueueRepository.enqueue(2L, baseScore + 1000)
            orderQueueRepository.enqueue(3L, baseScore + 2000)

            // when
            val position1 = orderQueueRepository.getPosition(1L)
            val position2 = orderQueueRepository.getPosition(2L)
            val position3 = orderQueueRepository.getPosition(3L)
            val positionNotExist = orderQueueRepository.getPosition(999L)

            // then
            assertThat(position1).isEqualTo(0L)
            assertThat(position2).isEqualTo(1L)
            assertThat(position3).isEqualTo(2L)
            assertThat(positionNotExist).isNull()
        }
    }

    @Nested
    @DisplayName("getTotalSize")
    inner class GetTotalSize {

        @Test
        @DisplayName("여러 유저 enqueue 후 getTotalSize가 정확한 인원을 반환한다")
        fun `여러 유저 enqueue 후 getTotalSize가 정확한 인원을 반환한다`() {
            // given
            val baseScore = System.currentTimeMillis().toDouble()
            orderQueueRepository.enqueue(1L, baseScore)
            orderQueueRepository.enqueue(2L, baseScore + 1000)
            orderQueueRepository.enqueue(3L, baseScore + 2000)

            // when
            val totalSize = orderQueueRepository.getTotalSize()

            // then
            assertThat(totalSize).isEqualTo(3L)
        }
    }

    @Nested
    @DisplayName("popFront")
    inner class PopFront {

        @Test
        @DisplayName("popFront(N)이 score 순서대로 N명을 반환하고 대기열에서 제거한다")
        fun `popFront(N)이 score 순서대로 N명을 반환하고 대기열에서 제거한다`() {
            // given
            val baseScore = System.currentTimeMillis().toDouble()
            orderQueueRepository.enqueue(3L, baseScore + 2000)
            orderQueueRepository.enqueue(1L, baseScore)
            orderQueueRepository.enqueue(2L, baseScore + 1000)

            // when
            val popped = orderQueueRepository.popFront(2)

            // then
            assertThat(popped).containsExactly(1L, 2L)
            assertThat(orderQueueRepository.getTotalSize()).isEqualTo(1L)
            assertThat(orderQueueRepository.getPosition(3L)).isEqualTo(0L)
        }

        @Test
        @DisplayName("빈 대기열에서 popFront 호출 시 빈 리스트를 반환한다")
        fun `빈 대기열에서 popFront 호출 시 빈 리스트를 반환한다`() {
            // given

            // when
            val popped = orderQueueRepository.popFront(5)

            // then
            assertThat(popped).isEmpty()
        }
    }
}
