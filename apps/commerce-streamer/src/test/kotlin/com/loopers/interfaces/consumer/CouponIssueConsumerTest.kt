package com.loopers.interfaces.consumer

import com.loopers.application.coupon.ProcessCouponIssueUseCase
import com.loopers.support.error.CoreException
import org.apache.kafka.clients.consumer.ConsumerRecord
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

    private fun createRecord(value: Any?): ConsumerRecord<Any, Any> {
        return ConsumerRecord(CouponIssueConsumer.TOPIC, 0, 0L, null as Any?, value as Any?)
    }

    @Nested
    @DisplayName("정상 처리 시")
    inner class NormalCase {

        @Test
        fun `COUPON_ISSUE_REQUESTED 이벤트가 오면 execute를 호출한다`() {
            // Arrange
            val payload = mapOf(
                "eventId" to "evt-1",
                "eventType" to CouponIssueConsumer.COUPON_ISSUE_REQUESTED,
                "couponId" to 10L,
                "userId" to 99L,
            )
            val record = createRecord(payload)

            // Act
            consumer.consume(record)

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
            val payload = mapOf(
                "eventId" to "evt-1",
                "eventType" to "UNKNOWN",
                "couponId" to 10L,
                "userId" to 99L,
            )
            val record = createRecord(payload)

            // Act
            consumer.consume(record)

            // Assert
            verifyNoInteractions(processCouponIssueUseCase)
        }
    }

    @Nested
    @DisplayName("페이로드 오류 시")
    inner class ErrorCase {

        @Test
        fun `payload가 null이면 CoreException을 던진다`() {
            // Arrange
            val record = createRecord(null)

            // Act & Assert
            assertThatThrownBy { consumer.consume(record) }
                .isInstanceOf(CoreException::class.java)
            verifyNoInteractions(processCouponIssueUseCase)
        }

        @Test
        fun `eventId가 누락되면 CoreException을 던진다`() {
            // Arrange
            val payload = mapOf(
                "eventType" to CouponIssueConsumer.COUPON_ISSUE_REQUESTED,
                "couponId" to 10L,
                "userId" to 99L,
            )
            val record = createRecord(payload)

            // Act & Assert
            assertThatThrownBy { consumer.consume(record) }
                .isInstanceOf(CoreException::class.java)
            verifyNoInteractions(processCouponIssueUseCase)
        }

        @Test
        fun `couponId가 누락되면 CoreException을 던진다`() {
            // Arrange
            val payload = mapOf(
                "eventId" to "evt-1",
                "eventType" to CouponIssueConsumer.COUPON_ISSUE_REQUESTED,
                "userId" to 99L,
            )
            val record = createRecord(payload)

            // Act & Assert
            assertThatThrownBy { consumer.consume(record) }
                .isInstanceOf(CoreException::class.java)
            verifyNoInteractions(processCouponIssueUseCase)
        }

        @Test
        fun `userId가 누락되면 CoreException을 던진다`() {
            // Arrange
            val payload = mapOf(
                "eventId" to "evt-1",
                "eventType" to CouponIssueConsumer.COUPON_ISSUE_REQUESTED,
                "couponId" to 10L,
            )
            val record = createRecord(payload)

            // Act & Assert
            assertThatThrownBy { consumer.consume(record) }
                .isInstanceOf(CoreException::class.java)
            verifyNoInteractions(processCouponIssueUseCase)
        }
    }

    @Nested
    @DisplayName("비즈니스 예외 전파 시")
    inner class ExceptionPropagation {

        @Test
        fun `execute에서 예외가 발생하면 그대로 전파된다`() {
            // Arrange
            val payload = mapOf(
                "eventId" to "evt-1",
                "eventType" to CouponIssueConsumer.COUPON_ISSUE_REQUESTED,
                "couponId" to 10L,
                "userId" to 99L,
            )
            val record = createRecord(payload)
            whenever(
                processCouponIssueUseCase.execute(
                    eventId = "evt-1",
                    couponId = 10L,
                    userId = 99L,
                ),
            ).doThrow(RuntimeException("비즈니스 예외"))

            // Act & Assert
            assertThatThrownBy { consumer.consume(record) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("비즈니스 예외")
        }
    }
}
