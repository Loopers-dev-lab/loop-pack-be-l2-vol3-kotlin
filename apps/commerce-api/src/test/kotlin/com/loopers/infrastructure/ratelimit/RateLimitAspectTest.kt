package com.loopers.infrastructure.ratelimit

import com.loopers.config.redis.RedisConfig
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class RateLimitAspectTest @Autowired constructor(
    private val rateLimitTestService: RateLimitTestService,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER) private val redisTemplate: RedisTemplate<*, *>,
) {

    @AfterEach
    fun tearDown() {
        @Suppress("UNCHECKED_CAST")
        val ops = (redisTemplate as RedisTemplate<String, String>)
        ops.keys("ratelimit:*").forEach { ops.delete(it) }
    }

    @Nested
    inner class ThrowOnDuplicate {

        @Test
        @DisplayName("throwOnDuplicate=true일 때 중복 요청 시 BAD_REQUEST 예외가 발생한다")
        fun duplicateThrowsException() {
            // arrange — 첫 번째 요청
            rateLimitTestService.throwOnDuplicateMethod(1L)

            // act — 중복 요청
            val result = assertThrows<CoreException> {
                rateLimitTestService.throwOnDuplicateMethod(1L)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("throwOnDuplicate=true일 때 다른 키는 정상 처리된다")
        fun differentKeySucceeds() {
            // arrange
            rateLimitTestService.throwOnDuplicateMethod(1L)

            // act & assert — 다른 키는 예외 없음
            rateLimitTestService.throwOnDuplicateMethod(2L)
        }

        @Test
        @DisplayName("throwOnDuplicate=true일 때 동시 요청 시 하나만 성공하고 나머지는 예외가 발생한다")
        fun concurrentRequestsOnlyOneSucceeds() {
            // arrange
            val threadCount = 10
            val executor = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            // act
            repeat(threadCount) {
                executor.submit {
                    try {
                        rateLimitTestService.throwOnDuplicateMethod(100L)
                        successCount.incrementAndGet()
                    } catch (e: CoreException) {
                        failCount.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executor.shutdown()

            // assert
            assertThat(successCount.get()).isEqualTo(1)
            assertThat(failCount.get()).isEqualTo(threadCount - 1)
        }
    }

    @Nested
    inner class SilentIgnore {

        @Test
        @DisplayName("throwOnDuplicate=false(기본)일 때 중복 요청 시 null을 반환한다")
        fun duplicateReturnsNull() {
            // arrange — 첫 번째 요청
            val first = rateLimitTestService.silentMethod(1L)

            // act — 중복 요청
            val second = rateLimitTestService.silentMethod(1L)

            // assert
            assertThat(first).isEqualTo("success")
            assertThat(second).isNull()
        }
    }
}
