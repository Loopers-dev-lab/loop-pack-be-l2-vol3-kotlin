package com.loopers.infrastructure.orderqueue

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate

@SpringBootTest
class OrderQueueRedisRepositoryTest @Autowired constructor(
    private val orderQueueRedisRepository: OrderQueueRedisRepository,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    companion object {
        private const val USER_ID_1 = 1L
        private const val USER_ID_2 = 2L
        private const val USER_ID_3 = 3L
        private const val QUEUE_KEY = "order:queue"
        private const val COUNTER_KEY = "order:queue:counter"
        private const val TOKEN_KEY_PREFIX = "order:token:"
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

    @DisplayName("enqueue")
    @Nested
    inner class Enqueue {
        @DisplayName("성공 시 1을 반환한다.")
        @Test
        fun returnsOneOnSuccess() {
            // act
            val result = orderQueueRedisRepository.enqueue(USER_ID_1)

            // assert
            assertThat(result).isEqualTo(1L)
        }

        @DisplayName("중복 enqueue 시 0을 반환한다.")
        @Test
        fun returnsZeroOnDuplicate() {
            // arrange
            orderQueueRedisRepository.enqueue(USER_ID_1)

            // act
            val result = orderQueueRedisRepository.enqueue(USER_ID_1)

            // assert
            assertThat(result).isEqualTo(0L)
        }

        @DisplayName("여러 유저가 enqueue하면 순서가 보장된다.")
        @Test
        fun preservesOrderForMultipleUsers() {
            // act
            orderQueueRedisRepository.enqueue(USER_ID_1)
            orderQueueRedisRepository.enqueue(USER_ID_2)
            orderQueueRedisRepository.enqueue(USER_ID_3)

            // assert
            val position1 = orderQueueRedisRepository.getPosition(USER_ID_1)
            val position2 = orderQueueRedisRepository.getPosition(USER_ID_2)
            val position3 = orderQueueRedisRepository.getPosition(USER_ID_3)

            assertAll(
                { assertThat(position1).isEqualTo(1L) },
                { assertThat(position2).isEqualTo(2L) },
                { assertThat(position3).isEqualTo(3L) },
            )
        }
    }

    @DisplayName("getPosition")
    @Nested
    inner class GetPosition {
        @DisplayName("대기열에 있는 유저의 순번을 반환한다. (1-based)")
        @Test
        fun returnsPositionForEnqueuedUser() {
            // arrange
            orderQueueRedisRepository.enqueue(USER_ID_1)
            orderQueueRedisRepository.enqueue(USER_ID_2)

            // act
            val position = orderQueueRedisRepository.getPosition(USER_ID_2)

            // assert
            assertThat(position).isEqualTo(2L)
        }

        @DisplayName("대기열에 없는 유저는 null을 반환한다.")
        @Test
        fun returnsNullForNonEnqueuedUser() {
            // act
            val position = orderQueueRedisRepository.getPosition(USER_ID_1)

            // assert
            assertThat(position).isNull()
        }
    }

    @DisplayName("getTotalSize")
    @Nested
    inner class GetTotalSize {
        @DisplayName("대기열의 전체 크기를 반환한다.")
        @Test
        fun returnsTotalSize() {
            // arrange
            orderQueueRedisRepository.enqueue(USER_ID_1)
            orderQueueRedisRepository.enqueue(USER_ID_2)

            // act
            val size = orderQueueRedisRepository.getTotalSize()

            // assert
            assertThat(size).isEqualTo(2L)
        }

        @DisplayName("비어있으면 0을 반환한다.")
        @Test
        fun returnsZeroWhenEmpty() {
            // act
            val size = orderQueueRedisRepository.getTotalSize()

            // assert
            assertThat(size).isEqualTo(0L)
        }
    }

    @DisplayName("dequeue")
    @Nested
    inner class Dequeue {
        @DisplayName("요청한 수만큼 유저를 꺼낸다.")
        @Test
        fun dequeuesRequestedCount() {
            // arrange
            orderQueueRedisRepository.enqueue(USER_ID_1)
            orderQueueRedisRepository.enqueue(USER_ID_2)
            orderQueueRedisRepository.enqueue(USER_ID_3)

            // act
            val dequeued = orderQueueRedisRepository.dequeue(2)

            // assert
            assertAll(
                { assertThat(dequeued).hasSize(2) },
                { assertThat(dequeued).containsExactly(USER_ID_1, USER_ID_2) },
                { assertThat(orderQueueRedisRepository.getTotalSize()).isEqualTo(1L) },
            )
        }

        @DisplayName("대기열이 비어있으면 빈 리스트를 반환한다.")
        @Test
        fun returnsEmptyListWhenQueueIsEmpty() {
            // act
            val dequeued = orderQueueRedisRepository.dequeue(5)

            // assert
            assertThat(dequeued).isEmpty()
        }
    }

    @DisplayName("Token 관리")
    @Nested
    inner class TokenManagement {
        @DisplayName("issueToken 후 hasToken이 true를 반환한다.")
        @Test
        fun hasTokenReturnsTrueAfterIssueToken() {
            // act
            orderQueueRedisRepository.issueToken(USER_ID_1, 300)

            // assert
            assertThat(orderQueueRedisRepository.hasToken(USER_ID_1)).isTrue()
        }

        @DisplayName("토큰이 없으면 hasToken이 false를 반환한다.")
        @Test
        fun hasTokenReturnsFalseWhenNoToken() {
            // act & assert
            assertThat(orderQueueRedisRepository.hasToken(USER_ID_1)).isFalse()
        }

        @DisplayName("consumeToken 성공 시 true를 반환하고 토큰이 삭제된다.")
        @Test
        fun consumeTokenReturnsTrueAndDeletesToken() {
            // arrange
            orderQueueRedisRepository.issueToken(USER_ID_1, 300)

            // act
            val consumed = orderQueueRedisRepository.consumeToken(USER_ID_1)

            // assert
            assertAll(
                { assertThat(consumed).isTrue() },
                { assertThat(orderQueueRedisRepository.hasToken(USER_ID_1)).isFalse() },
            )
        }

        @DisplayName("토큰이 없을 때 consumeToken은 false를 반환한다.")
        @Test
        fun consumeTokenReturnsFalseWhenNoToken() {
            // act
            val consumed = orderQueueRedisRepository.consumeToken(USER_ID_1)

            // assert
            assertThat(consumed).isFalse()
        }

        @DisplayName("issueToken의 TTL이 적용된다.")
        @Test
        fun issueTokenAppliesTtl() {
            // act
            orderQueueRedisRepository.issueToken(USER_ID_1, 300)

            // assert
            val ttl = redisTemplate.getExpire("${TOKEN_KEY_PREFIX}${USER_ID_1}")
            assertThat(ttl).isGreaterThan(0L)
            assertThat(ttl).isLessThanOrEqualTo(300L)
        }

        @DisplayName("getTokenTtl은 토큰의 남은 TTL을 반환한다.")
        @Test
        fun getTokenTtlReturnsTtl() {
            // arrange
            orderQueueRedisRepository.issueToken(USER_ID_1, 300)

            // act
            val ttl = orderQueueRedisRepository.getTokenTtl(USER_ID_1)

            // assert
            assertThat(ttl).isGreaterThan(0L)
            assertThat(ttl).isLessThanOrEqualTo(300L)
        }

        @DisplayName("토큰이 없으면 getTokenTtl은 -2를 반환한다.")
        @Test
        fun getTokenTtlReturnsNegativeTwoWhenNoToken() {
            // act
            val ttl = orderQueueRedisRepository.getTokenTtl(USER_ID_1)

            // assert
            assertThat(ttl).isEqualTo(-2L)
        }
    }
}
