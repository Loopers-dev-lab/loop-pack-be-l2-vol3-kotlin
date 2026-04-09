package com.loopers.interfaces.consumer

import com.loopers.application.coupon.ProcessCouponIssueUseCase
import com.loopers.interfaces.consumer.dto.CouponIssuePayload
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@DisplayName("CouponIssueConsumer")
class CouponIssueConsumerTest {

    private lateinit var processCouponIssueUseCase: ProcessCouponIssueUseCase
    private lateinit var consumer: CouponIssueConsumer

    @BeforeEach
    fun setUp() {
        processCouponIssueUseCase = mock()
        consumer = CouponIssueConsumer(processCouponIssueUseCase)
    }

    @Nested
    @DisplayName("정상 처리 시")
    inner class NormalCase {

        @Test
        fun `COUPON_ISSUE_REQUESTED 이벤트가 오면 execute를 호출한다`() {
            // Arrange
            val payload = CouponIssuePayload(
                eventId = "evt-1",
                eventType = CouponIssueConsumer.COUPON_ISSUE_REQUESTED,
                couponId = 10L,
                userId = 99L,
            )

            // Act
            consumer.consume(payload)

            // Assert
            verify(processCouponIssueUseCase).execute(
                eventId = "evt-1",
                couponId = 10L,
                userId = 99L,
            )
        }

        @Test
        fun `알 수 없는 eventType이 오면 예외 없이 정상 종료한다`() {
            // Arrange
            val payload = CouponIssuePayload(
                eventId = "evt-1",
                eventType = "UNKNOWN",
                couponId = 10L,
                userId = 99L,
            )

            // Act
            consumer.consume(payload)

            // Assert
            verifyNoInteractions(processCouponIssueUseCase)
        }
    }

    @Nested
    @DisplayName("비즈니스 예외 전파 시")
    inner class ExceptionPropagation {

        @Test
        fun `execute에서 예외가 발생하면 그대로 전파된다`() {
            // Arrange
            val payload = CouponIssuePayload(
                eventId = "evt-1",
                eventType = CouponIssueConsumer.COUPON_ISSUE_REQUESTED,
                couponId = 10L,
                userId = 99L,
            )
            whenever(
                processCouponIssueUseCase.execute(
                    eventId = "evt-1",
                    couponId = 10L,
                    userId = 99L,
                ),
            ).doThrow(RuntimeException("비즈니스 예외"))

            // Act & Assert
            assertThatThrownBy { consumer.consume(payload) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("비즈니스 예외")
        }
    }
}
