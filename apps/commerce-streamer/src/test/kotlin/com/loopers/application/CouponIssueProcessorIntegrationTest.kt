package com.loopers.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.config.redis.RedisConfig
import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponIssueRequest
import com.loopers.domain.coupon.CouponIssueStatus
import com.loopers.domain.coupon.CouponQuantity
import com.loopers.domain.coupon.Discount
import com.loopers.domain.coupon.DiscountType
import com.loopers.event.EventEnvelope
import com.loopers.event.payload.CouponIssueRequestPayload
import com.loopers.infrastructure.coupon.CouponIssueRequestJpaRepository
import com.loopers.infrastructure.coupon.CouponJpaRepository
import com.loopers.infrastructure.coupon.IssuedCouponJpaRepository
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import java.time.Instant
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@SpringBootTest
class CouponIssueProcessorIntegrationTest @Autowired constructor(
    private val couponIssueProcessor: CouponIssueProcessor,
    private val couponJpaRepository: CouponJpaRepository,
    private val couponIssueRequestJpaRepository: CouponIssueRequestJpaRepository,
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
    private val objectMapper: ObjectMapper,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    private fun createCoupon(totalQuantity: Int = 100): Coupon {
        return couponJpaRepository.save(
            Coupon(
                name = "테스트 쿠폰",
                discount = Discount(DiscountType.FIXED_AMOUNT, 5000L),
                quantity = CouponQuantity(totalQuantity, 0),
                expiresAt = ZonedDateTime.now().plusDays(30),
            ),
        )
    }

    private fun createIssueRequest(requestId: String, couponId: Long, userId: Long): CouponIssueRequest {
        return couponIssueRequestJpaRepository.save(
            CouponIssueRequest(requestId = requestId, couponId = couponId, userId = userId),
        )
    }

    private fun createEnvelope(
        eventId: String,
        couponId: Long,
        userId: Long,
        requestId: String,
        version: Long = 1L,
    ): EventEnvelope {
        val payload = CouponIssueRequestPayload(
            couponId = couponId,
            userId = userId,
            requestId = requestId,
        )
        return EventEnvelope(
            eventId = eventId,
            eventType = "COUPON_ISSUE_REQUESTED",
            aggregateId = couponId.toString(),
            version = version,
            timestamp = Instant.now(),
            payload = objectMapper.writeValueAsString(payload),
        )
    }

    private fun addToRedisIssuedSet(couponId: Long, userId: Long) {
        redisTemplate.opsForSet().add("coupon-issued:$couponId", userId.toString())
    }

    private fun isInRedisIssuedSet(couponId: Long, userId: Long): Boolean {
        return redisTemplate.opsForSet()
            .isMember("coupon-issued:$couponId", userId.toString()) ?: false
    }

    @DisplayName("멱등성 통합 테스트:")
    @Nested
    inner class IdempotencyIntegration {

        @DisplayName("같은 eventId를 두 번 처리하면, 쿠폰은 1회만 발급되고 상태는 ISSUED이다.")
        @Test
        fun processesOnlyOnce() {
            // arrange
            val coupon = createCoupon()
            val issueRequest = createIssueRequest("req-dedup", coupon.id, 1L)
            val envelope = createEnvelope("evt-dedup", coupon.id, 1L, "req-dedup")

            // act
            couponIssueProcessor.process(envelope)
            couponIssueProcessor.process(envelope)

            // assert
            val issuedCount = issuedCouponJpaRepository.findAll().size
            assertThat(issuedCount).isEqualTo(1)

            val updatedRequest = couponIssueRequestJpaRepository.findByRequestId("req-dedup")
            assertThat(updatedRequest!!.status).isEqualTo(CouponIssueStatus.ISSUED)
        }
    }

    @DisplayName("동시성 멱등성 테스트:")
    @Nested
    inner class ConcurrentIdempotency {

        @DisplayName("같은 eventId를 여러 스레드에서 동시에 처리하면, 쿠폰은 1회만 발급된다.")
        @Test
        fun processesOnlyOnceUnderConcurrency() {
            // arrange
            val coupon = createCoupon()
            createIssueRequest("req-concurrent", coupon.id, 1L)
            val envelope = createEnvelope("evt-concurrent", coupon.id, 1L, "req-concurrent")
            val threadCount = 10
            val executor = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)

            // act
            repeat(threadCount) {
                executor.submit {
                    try {
                        couponIssueProcessor.process(envelope)
                    } catch (_: Exception) {
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executor.shutdown()

            // assert
            val issuedCount = issuedCouponJpaRepository.findAll().size
            assertThat(issuedCount).isEqualTo(1)

            val updatedRequest = couponIssueRequestJpaRepository.findByRequestId("req-concurrent")
            assertThat(updatedRequest!!.status).isEqualTo(CouponIssueStatus.ISSUED)
        }
    }

    @DisplayName("Redis 복구 테스트:")
    @Nested
    inner class RedisRestore {

        @DisplayName("발급 실패 시 Redis에서 사용자가 제거되어 재요청이 가능하다.")
        @Test
        fun restoresRedisOnFailure() {
            // arrange — 만료된 쿠폰으로 발급 실패 유도
            val expiredCoupon = couponJpaRepository.save(
                Coupon(
                    name = "만료 쿠폰",
                    discount = Discount(DiscountType.FIXED_AMOUNT, 1000L),
                    quantity = CouponQuantity(100, 0),
                    expiresAt = ZonedDateTime.now().minusDays(1),
                ),
            )
            createIssueRequest("req-fail", expiredCoupon.id, 1L)
            addToRedisIssuedSet(expiredCoupon.id, 1L)
            val envelope = createEnvelope("evt-fail", expiredCoupon.id, 1L, "req-fail")

            // act
            couponIssueProcessor.process(envelope)

            // assert — Redis에서 사용자가 제거됨
            assertThat(isInRedisIssuedSet(expiredCoupon.id, 1L)).isFalse()

            // assert — 요청 상태가 FAILED로 전이됨
            val updatedRequest = couponIssueRequestJpaRepository.findByRequestId("req-fail")
            assertThat(updatedRequest!!.status).isEqualTo(CouponIssueStatus.FAILED)
            assertThat(updatedRequest.failReason).isNotNull()
        }

        @DisplayName("중복 발급 시도 시 Redis에서 사용자가 제거되어 재요청이 가능하다.")
        @Test
        fun restoresRedisOnDuplicateIssue() {
            // arrange — 이미 발급된 쿠폰이 있는 상태
            val coupon = createCoupon()
            issuedCouponJpaRepository.save(
                com.loopers.domain.coupon.IssuedCoupon(couponId = coupon.id, userId = 1L),
            )
            createIssueRequest("req-dup", coupon.id, 1L)
            addToRedisIssuedSet(coupon.id, 1L)
            val envelope = createEnvelope("evt-dup", coupon.id, 1L, "req-dup")

            // act
            couponIssueProcessor.process(envelope)

            // assert — Redis에서 사용자가 제거됨
            assertThat(isInRedisIssuedSet(coupon.id, 1L)).isFalse()

            // assert — 요청 상태가 FAILED로 전이됨
            val updatedRequest = couponIssueRequestJpaRepository.findByRequestId("req-dup")
            assertThat(updatedRequest!!.status).isEqualTo(CouponIssueStatus.FAILED)
            assertThat(updatedRequest.failReason).contains("이미 발급된 쿠폰")
        }
    }
}
