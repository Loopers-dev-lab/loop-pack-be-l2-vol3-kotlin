package com.loopers.application.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponIssueRepository
import com.loopers.domain.coupon.CouponIssueRequestRepository
import com.loopers.domain.coupon.CouponIssueStatus
import com.loopers.domain.coupon.CouponQuantity
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.Discount
import com.loopers.domain.coupon.DiscountType
import com.loopers.infrastructure.outbox.OutboxEventRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class CouponIssueFacadeIntegrationTest @Autowired constructor(
    private val couponIssueFacade: CouponIssueFacade,
    private val couponRepository: CouponRepository,
    private val couponIssueRepository: CouponIssueRepository,
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {

    private fun createCoupon(
        totalQuantity: Int = 100,
        expiresAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ): Coupon {
        return couponRepository.save(
            Coupon(
                name = "선���순 쿠폰",
                discount = Discount(DiscountType.FIXED_AMOUNT, 5000L),
                quantity = CouponQuantity(totalQuantity, 0),
                expiresAt = expiresAt,
            ),
        )
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    @Nested
    @DisplayName("issueAsync")
    inner class IssueAsync {

        @Test
        @DisplayName("존재하지 않는 쿠폰 ID로 요청 시 NOT_FOUND 예외가 발생한다")
        fun `존재하지 않는 쿠폰 ID로 요청 시 NOT_FOUND 예외가 발생한다`() {
            // given
            val nonExistentCouponId = 999L
            val userId = 1L

            // when & then
            val exception = assertThrows<CoreException> {
                couponIssueFacade.issueAsync(nonExistentCouponId, userId)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("만료된 쿠폰으로 요청 시 BAD_REQUEST 예외가 발생한다")
        fun `만료된 쿠폰으로 요청 시 BAD_REQUEST 예외가 발생한다`() {
            // given
            val expiredCoupon = createCoupon(expiresAt = ZonedDateTime.now().minusDays(1))
            val userId = 1L

            // when & then
            val exception = assertThrows<CoreException> {
                couponIssueFacade.issueAsync(expiredCoupon.id, userId)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("같은 유저가 중복 요청하면 CONFLICT 예외가 발생한다")
        fun `같은 유저가 중복 요청하면 CONFLICT 예외가 발생한다`() {
            // given
            val coupon = createCoupon()
            val userId = 1L
            couponIssueRepository.tryIssue(coupon.id, userId, coupon.quantity.total)

            // when & then
            val exception = assertThrows<CoreException> {
                couponIssueFacade.issueAsync(coupon.id, userId)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @Test
        @DisplayName("수량이 소진된 쿠폰으로 요청 시 BAD_REQUEST 예외가 발생한다")
        fun `수량이 소진된 쿠폰으로 요청 시 BAD_REQUEST 예외가 발생한다`() {
            // given
            val coupon = createCoupon(totalQuantity = 1)
            couponIssueRepository.tryIssue(coupon.id, 99L, coupon.quantity.total)

            // when & then
            val exception = assertThrows<CoreException> {
                couponIssueFacade.issueAsync(coupon.id, 2L)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("정상 요청 시 PENDING 상태로 저장하고 requestId를 반환한다")
        fun `정상 요청 시 PENDING 상태로 저장하고 requestId를 반환한다`() {
            // given
            val coupon = createCoupon()
            val userId = 1L

            // when
            val requestId = couponIssueFacade.issueAsync(coupon.id, userId)

            // then
            val issueRequest = couponIssueRequestRepository.findByRequestId(requestId)
            assertThat(issueRequest).isNotNull
            assertThat(issueRequest!!.couponId).isEqualTo(coupon.id)
            assertThat(issueRequest.userId).isEqualTo(userId)
            assertThat(issueRequest.status).isEqualTo(CouponIssueStatus.PENDING)

            val outboxEvents = outboxEventRepository.findByPublishedAtIsNull()
            assertThat(outboxEvents).hasSize(1)
            assertThat(outboxEvents[0].eventType).isEqualTo("COUPON_ISSUE_REQUESTED")
            assertThat(outboxEvents[0].aggregateId).isEqualTo(coupon.id.toString())
        }
    }

    @Nested
    @DisplayName("getIssueRequest")
    inner class GetIssueRequest {

        @Test
        @DisplayName("존재하지 않는 requestId로 조회 시 NOT_FOUND 예외가 발생한다")
        fun `존재하지 않는 requestId로 조회 시 NOT_FOUND 예외가 발생한다`() {
            // given
            val nonExistentRequestId = "non-existent-request-id"
            val userId = 1L

            // when & then
            val exception = assertThrows<CoreException> {
                couponIssueFacade.getIssueRequest(nonExistentRequestId, userId)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("존재하는 requestId로 본인이 조회 시 요청 상태 정보를 반환한다")
        fun `존재하는 requestId로 본인이 조회 시 요청 상태 정보를 반환한다`() {
            // given
            val coupon = createCoupon()
            val userId = 1L
            val requestId = couponIssueFacade.issueAsync(coupon.id, userId)

            // when
            val info = couponIssueFacade.getIssueRequest(requestId, userId)

            // then
            assertThat(info.requestId).isEqualTo(requestId)
            assertThat(info.couponId).isEqualTo(coupon.id)
            assertThat(info.status).isEqualTo(CouponIssueStatus.PENDING)
            assertThat(info.failReason).isNull()
            assertThat(info.createdAt).isNotNull()
        }

        @Test
        @DisplayName("다른 사용자의 requestId로 조회 시 NOT_FOUND 예외가 발생한다")
        fun `다른 사용자의 requestId로 조회 시 NOT_FOUND 예외가 발생한다`() {
            // given
            val coupon = createCoupon()
            val ownerId = 1L
            val otherId = 2L
            val requestId = couponIssueFacade.issueAsync(coupon.id, ownerId)

            // when & then
            val exception = assertThrows<CoreException> {
                couponIssueFacade.getIssueRequest(requestId, otherId)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("동시성 — 수량 초과 방지")
    inner class ConcurrencyQuantity {

        @Test
        @DisplayName("쿠폰 100장에 200명이 동시에 요청하면 정확히 100건만 성공한다")
        fun `쿠폰 100장에 200명이 동시에 요청하면 정확히 100건만 성공한다`() {
            // given
            val coupon = createCoupon(totalQuantity = 100)
            val totalRequests = 200
            val executor = Executors.newFixedThreadPool(32)
            val latch = CountDownLatch(totalRequests)
            val successCount = AtomicInteger(0)

            // when
            for (userId in 1L..totalRequests.toLong()) {
                executor.submit {
                    try {
                        couponIssueFacade.issueAsync(coupon.id, userId)
                        successCount.incrementAndGet()
                    } catch (_: CoreException) {
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executor.shutdown()

            // then
            assertThat(successCount.get()).isEqualTo(100)
        }
    }

    @Nested
    @DisplayName("동시성 — 중복 발급 방지")
    inner class ConcurrencyDuplicate {

        @Test
        @DisplayName("같은 유저가 동시에 10번 요청하면 1건만 성공한다")
        fun `같은 유저가 동시에 10번 요청하면 1건만 성공한다`() {
            // given
            val coupon = createCoupon()
            val userId = 1L
            val threadCount = 10
            val executor = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val successCount = AtomicInteger(0)

            // when
            repeat(threadCount) {
                executor.submit {
                    try {
                        couponIssueFacade.issueAsync(coupon.id, userId)
                        successCount.incrementAndGet()
                    } catch (_: CoreException) {
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executor.shutdown()

            // then
            assertThat(successCount.get()).isEqualTo(1)
        }
    }
}
