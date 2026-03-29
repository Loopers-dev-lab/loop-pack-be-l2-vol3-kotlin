package com.loopers.application.coupon

import com.loopers.application.event.OutboxEventWriter
import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponIssueRequest
import com.loopers.domain.coupon.CouponIssueRequestReader
import com.loopers.domain.coupon.CouponIssueRequestRegister
import com.loopers.domain.coupon.CouponIssueRequestRepository
import com.loopers.domain.coupon.CouponReader
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.coupon.IssuedCouponReader
import com.loopers.domain.coupon.IssuedCouponRepository
import com.loopers.domain.coupon.vo.CouponName
import com.loopers.domain.coupon.vo.DiscountValue
import com.loopers.domain.coupon.vo.IssueLimit
import com.loopers.domain.coupon.vo.MinOrderAmount
import com.loopers.kafka.KafkaTopics
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.ZonedDateTime

class CouponUseCaseTest {

    @Test
    fun `쿠폰_발급은_즉시_발급하지_않고_PENDING_요청을_생성한다`() {
        val requestRepository = FakeCouponIssueRequestRepository()
        val outboxEventWriter = mockk<OutboxEventWriter>(relaxed = true)
        val applicationEventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)

        val useCase = CouponUseCase(
            couponIssueRequestRegister = CouponIssueRequestRegister(requestRepository),
            couponIssueRequestReader = CouponIssueRequestReader(requestRepository),
            couponReader = CouponReader(FakeCouponRepository()),
            issuedCouponReader = IssuedCouponReader(FakeIssuedCouponRepository()),
            applicationEventPublisher = applicationEventPublisher,
            outboxEventWriter = outboxEventWriter,
        )

        val result = useCase.issueCoupon(couponId = 1L, memberId = 99L)

        assertThat(result.requestId).isPositive()
        assertThat(result.couponId).isEqualTo(1L)
        assertThat(result.memberId).isEqualTo(99L)
        assertThat(result.status).isEqualTo("PENDING")
        verify(exactly = 1) { outboxEventWriter.append(eq(KafkaTopics.COUPON_ISSUE_REQUESTS), any()) }
    }

    private class FakeCouponIssueRequestRepository : CouponIssueRequestRepository {
        private val store = linkedMapOf<Long, CouponIssueRequest>()
        private var sequence = 1L

        override fun save(request: CouponIssueRequest): CouponIssueRequest {
            val persisted = if (request.id == null) {
                CouponIssueRequest(
                    id = sequence++,
                    couponId = request.couponId,
                    memberId = request.memberId,
                    status = request.status,
                    requestedAt = request.requestedAt,
                    issuedCouponId = request.issuedCouponId,
                    failureReason = request.failureReason,
                )
            } else {
                request
            }
            store[requireNotNull(persisted.id)] = persisted
            return persisted
        }

        override fun findById(id: Long): CouponIssueRequest? = store[id]
    }

    private class FakeCouponRepository : CouponRepository {
        private val coupon = Coupon(
            id = 1L,
            name = CouponName("선착순 쿠폰"),
            type = CouponType.FIXED,
            discountValue = DiscountValue(3000L),
            minOrderAmount = MinOrderAmount(null),
            expiredAt = ZonedDateTime.now().plusDays(1),
            issueLimit = IssueLimit(100L),
            issuedCount = 0L,
        )

        override fun save(coupon: Coupon): Coupon = coupon

        override fun findById(id: Long): Coupon? = if (id == coupon.id) coupon else null

        override fun findAllByIds(ids: List<Long>): List<Coupon> = if (coupon.id in ids) listOf(coupon) else emptyList()

        override fun findAll(pageable: Pageable): Page<Coupon> = Page.empty()

        override fun tryIncreaseIssuedCount(id: Long): Int = 1

        override fun deleteById(id: Long) = Unit
    }

    private class FakeIssuedCouponRepository : IssuedCouponRepository {
        override fun save(issuedCoupon: IssuedCoupon): IssuedCoupon = issuedCoupon
        override fun findById(id: Long): IssuedCoupon? = null
        override fun findByIdForUpdate(id: Long): IssuedCoupon? = null
        override fun findAllByMemberId(memberId: Long): List<IssuedCoupon> = emptyList()
        override fun existsByCouponIdAndMemberId(couponId: Long, memberId: Long): Boolean = false
        override fun findAllByCouponId(couponId: Long, pageable: Pageable): Page<IssuedCoupon> = Page.empty()
    }
}
