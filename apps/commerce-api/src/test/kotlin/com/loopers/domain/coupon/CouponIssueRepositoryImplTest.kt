package com.loopers.domain.coupon

import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

@SpringBootTest
class CouponIssueRepositoryImplTest @Autowired constructor(
    private val couponIssueRepository: CouponIssueRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val redisCleanUp: RedisCleanUp,
) {

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @Nested
    @DisplayName("tryIssue")
    inner class TryIssue {

        @Test
        @DisplayName("정상 요청 시 1을 반환한다")
        fun `정상 요청 시 1을 반환한다`() {
            // given
            val couponId = 1L
            val userId = 100L
            val maxQuantity = 10

            // when
            val result = couponIssueRepository.tryIssue(couponId, userId, maxQuantity)

            // then
            assertThat(result).isEqualTo(1L)
        }

        @Test
        @DisplayName("같은 유저가 중복 요청하면 -1을 반환한다")
        fun `같은 유저가 중복 요청하면 -1을 반환한다`() {
            // given
            val couponId = 1L
            val userId = 100L
            val maxQuantity = 10
            couponIssueRepository.tryIssue(couponId, userId, maxQuantity)

            // when
            val result = couponIssueRepository.tryIssue(couponId, userId, maxQuantity)

            // then
            assertThat(result).isEqualTo(-1L)
        }

        @Test
        @DisplayName("수량이 소진되면 0을 반환한다")
        fun `수량이 소진되면 0을 반환한다`() {
            // given
            val couponId = 1L
            val maxQuantity = 3
            couponIssueRepository.tryIssue(couponId, 1L, maxQuantity)
            couponIssueRepository.tryIssue(couponId, 2L, maxQuantity)
            couponIssueRepository.tryIssue(couponId, 3L, maxQuantity)

            // when
            val result = couponIssueRepository.tryIssue(couponId, 4L, maxQuantity)

            // then
            assertThat(result).isEqualTo(0L)
        }
    }

    @Nested
    @DisplayName("restore")
    inner class Restore {

        @Test
        @DisplayName("발급된 유저를 제거하면 해당 유저가 다시 발급받을 수 있다")
        fun `발급된 유저를 제거하면 해당 유저가 다시 발급받을 수 있다`() {
            // given
            val couponId = 1L
            val userId = 100L
            val maxQuantity = 10
            couponIssueRepository.tryIssue(couponId, userId, maxQuantity)

            // when
            couponIssueRepository.restore(couponId, userId)

            // then
            val result = couponIssueRepository.tryIssue(couponId, userId, maxQuantity)
            assertThat(result).isEqualTo(1L)
        }
    }

    @Nested
    @DisplayName("동시성")
    inner class Concurrency {

        @Test
        @DisplayName("동시에 200명이 요청해도 maxQuantity(100)명만 성공한다")
        fun `동시에 200명이 요청해도 maxQuantity명만 성공한다`() {
            // given
            val couponId = 1L
            val maxQuantity = 100
            val totalRequests = 200
            val executor = Executors.newFixedThreadPool(32)
            val latch = CountDownLatch(totalRequests)
            val successCount = AtomicLong(0)

            // when
            for (userId in 1L..totalRequests) {
                executor.submit {
                    try {
                        val result = couponIssueRepository.tryIssue(couponId, userId, maxQuantity)
                        if (result == 1L) successCount.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executor.shutdown()

            // then
            assertThat(successCount.get()).isEqualTo(maxQuantity.toLong())
        }
    }

    @Nested
    @DisplayName("initCouponStock")
    inner class InitCouponStock {

        @Test
        @DisplayName("maxQuantity를 Redis에 저장한다")
        fun `maxQuantity를 Redis에 저장한다`() {
            // given
            val couponId = 1L
            val maxQuantity = 100

            // when
            couponIssueRepository.initCouponStock(couponId, maxQuantity)

            // then
            val stored = redisTemplate.opsForValue().get("coupon-max:$couponId")
            assertThat(stored).isEqualTo("100")
        }
    }
}
