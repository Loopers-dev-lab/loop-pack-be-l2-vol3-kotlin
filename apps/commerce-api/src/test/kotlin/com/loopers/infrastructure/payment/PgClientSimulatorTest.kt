package com.loopers.infrastructure.payment

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PgClientSimulator")
class PgClientSimulatorTest {

    companion object {
        private const val ORDER_ID = 1L
        private const val PAYMENT_AMOUNT = 50_000L
    }

    private fun createRequest(): PgPaymentRequest {
        return PgPaymentRequest(
            orderId = ORDER_ID,
            amount = PAYMENT_AMOUNT,
        )
    }

    @DisplayName("requestPayment")
    @Nested
    inner class RequestPayment {
        @DisplayName("성공 시나리오면 승인 응답으로 매핑한다")
        @Test
        fun returnsApprovalResponse_whenSimulatorIsConfiguredToSucceed() {
            val pgClient = PgClientSimulator(PgClientSimulator.Scenario.SUCCESS)

            val result = pgClient.requestPayment(createRequest())

            assertThat(result.orderId).isEqualTo(ORDER_ID)
            assertThat(result.amount).isEqualTo(PAYMENT_AMOUNT)
            assertThat(result.status).isEqualTo(PgPaymentStatus.APPROVED)
            assertThat(result.transactionId).startsWith("pg-")
        }

        @DisplayName("실패 시나리오면 CONFLICT 내부 예외로 매핑한다")
        @Test
        fun throwsConflict_whenSimulatorMapsDeclinedPgFailure() {
            val pgClient = PgClientSimulator(PgClientSimulator.Scenario.FAILURE)

            assertThatThrownBy {
                pgClient.requestPayment(createRequest())
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.CONFLICT)
                .hasMessageContaining("카드 승인에 실패했습니다")
        }

        @DisplayName("타임아웃 시나리오면 INTERNAL_ERROR 내부 예외로 매핑한다")
        @Test
        fun throwsInternalError_whenSimulatorMapsTimeout() {
            val pgClient = PgClientSimulator(PgClientSimulator.Scenario.TIMEOUT)

            assertThatThrownBy {
                pgClient.requestPayment(createRequest())
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INTERNAL_ERROR)
                .hasMessageContaining("응답 시간이 초과되었습니다")
        }
    }
}
