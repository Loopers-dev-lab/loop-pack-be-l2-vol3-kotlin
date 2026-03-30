package com.loopers.application.coupon

import com.loopers.domain.common.vo.CouponId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.coupon.FakeCouponIssueRequestRepository
import com.loopers.domain.outbox.FakeCouponOutboxRepository
import com.loopers.domain.outbox.model.CouponOutboxEventType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RequestCouponIssueUseCaseTest {

    private lateinit var couponIssueRequestRepository: FakeCouponIssueRequestRepository
    private lateinit var couponOutboxRepository: FakeCouponOutboxRepository
    private lateinit var useCase: RequestCouponIssueUseCase

    @BeforeEach
    fun setUp() {
        couponIssueRequestRepository = FakeCouponIssueRequestRepository()
        couponOutboxRepository = FakeCouponOutboxRepository()
        useCase = RequestCouponIssueUseCase(couponIssueRequestRepository, couponOutboxRepository)
    }

    @Nested
    @DisplayName("execute 시")
    inner class Execute {

        @Test
        fun `CouponIssueRequest가 PENDING 상태로 저장되고 requestId가 반환된다`() {
            val result = useCase.execute(userId = 1L, couponId = 10L)

            assertThat(result.requestId).isNotBlank()

            val saved = couponIssueRequestRepository.findByRequestId(result.requestId)
            assertThat(saved).isNotNull()
            val found = requireNotNull(saved)
            assertThat(found.status.name).isEqualTo("PENDING")
            assertThat(found.couponId).isEqualTo(CouponId(10L))
            assertThat(found.userId).isEqualTo(UserId(1L))
        }

        @Test
        fun `CouponOutbox가 함께 기록된다`() {
            val result = useCase.execute(userId = 1L, couponId = 10L)

            val outboxList = couponOutboxRepository.findAllUnpublished()
            assertThat(outboxList).hasSize(1)

            val outbox = outboxList[0]
            assertThat(outbox.eventId).isEqualTo(result.requestId)
            assertThat(outbox.eventType).isEqualTo(CouponOutboxEventType.COUPON_ISSUE_REQUESTED)
            assertThat(outbox.couponId).isEqualTo(CouponId(10L))
            assertThat(outbox.userId).isEqualTo(UserId(1L))
        }
    }
}
