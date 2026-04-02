package com.loopers.infrastructure.orderqueue

import com.loopers.domain.orderqueue.OrderQueueService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate

@SpringBootTest
class OrderQueueTokenSchedulerTest @Autowired constructor(
    private val orderQueueRedisRepository: OrderQueueRedisRepository,
    private val orderQueueService: OrderQueueService,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    companion object {
        private const val QUEUE_KEY = "order:queue"
        private const val COUNTER_KEY = "order:queue:counter"
        private const val TOKEN_KEY_PREFIX = "order:token:"
        private const val BATCH_SIZE = 7L
    }

    @BeforeEach
    fun setUp() {
        cleanUpRedis()
    }

    @AfterEach
    fun tearDown() {
        cleanUpRedis()
    }

    private fun cleanUpRedis() {
        redisTemplate.delete(QUEUE_KEY)
        redisTemplate.delete(COUNTER_KEY)
        redisTemplate.keys("$TOKEN_KEY_PREFIX*")?.forEach { redisTemplate.delete(it) }
    }

    @DisplayName("토큰 발급 처리")
    @Nested
    inner class ProcessTokenIssuance {
        @DisplayName("대기열에 유저가 있으면 토큰이 발급된다.")
        @Test
        fun issuesTokensWhenUsersInQueue() {
            // arrange
            orderQueueRedisRepository.enqueue(1L)
            orderQueueRedisRepository.enqueue(2L)
            orderQueueRedisRepository.enqueue(3L)

            // act
            orderQueueService.processTokenIssuance(BATCH_SIZE)

            // assert
            assertThat(orderQueueRedisRepository.hasToken(1L)).isTrue()
            assertThat(orderQueueRedisRepository.hasToken(2L)).isTrue()
            assertThat(orderQueueRedisRepository.hasToken(3L)).isTrue()
            assertThat(orderQueueRedisRepository.getTotalSize()).isEqualTo(0L)
        }

        @DisplayName("대기열이 비어있으면 아무 일도 일어나지 않는다.")
        @Test
        fun doesNothingWhenQueueIsEmpty() {
            // act
            orderQueueService.processTokenIssuance(BATCH_SIZE)

            // assert
            assertThat(orderQueueRedisRepository.getTotalSize()).isEqualTo(0L)
        }

        @DisplayName("배치 크기보다 대기열이 크면 배치 크기만큼만 처리한다.")
        @Test
        fun processesOnlyBatchSizeWhenQueueIsLarger() {
            // arrange
            for (i in 1L..10L) {
                orderQueueRedisRepository.enqueue(i)
            }

            // act
            orderQueueService.processTokenIssuance(BATCH_SIZE)

            // assert
            assertThat(orderQueueRedisRepository.getTotalSize()).isEqualTo(3L)
            var tokenCount = 0L
            for (i in 1L..10L) {
                if (orderQueueRedisRepository.hasToken(i)) tokenCount++
            }
            assertThat(tokenCount).isEqualTo(7L)
        }
    }
}
