package com.loopers.infrastructure.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import java.time.Duration

@SpringBootTest
class OrderRateLimiterTest @Autowired constructor(
    private val orderRateLimiter: OrderRateLimiter,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    companion object {
        private const val RATE_LIMIT_KEY = "order:rate-limit"
    }

    @BeforeEach
    fun setUp() {
        redisTemplate.delete(RATE_LIMIT_KEY)
    }

    @AfterEach
    fun tearDown() {
        redisTemplate.delete(RATE_LIMIT_KEY)
    }

    @DisplayName("checkRate")
    @Nested
    inner class CheckRate {
        @DisplayName("제한 이내의 요청은 예외 없이 통과한다.")
        @Test
        fun allowsRequestsWithinLimit() {
            // act & assert
            assertDoesNotThrow {
                repeat(100) { orderRateLimiter.checkRate() }
            }
        }

        @DisplayName("제한을 초과하면 TOO_MANY_REQUESTS 예외를 던진다.")
        @Test
        fun throwsExceptionWhenExceedingLimit() {
            // arrange
            repeat(100) { orderRateLimiter.checkRate() }

            // act
            val exception = assertThrows<CoreException> {
                orderRateLimiter.checkRate()
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.TOO_MANY_REQUESTS)
        }

        @DisplayName("윈도우가 지나면 다시 요청이 허용된다.")
        @Test
        fun allowsRequestsAfterWindowExpires() {
            // arrange
            repeat(100) { orderRateLimiter.checkRate() }

            // act & assert - 윈도우(1초) 만료 후 통과 확인
            await atMost Duration.ofSeconds(3) untilAsserted {
                assertDoesNotThrow { orderRateLimiter.checkRate() }
            }
        }
    }
}
