package com.loopers.interfaces.api.payment

import com.loopers.application.payment.PaymentCallbackCommand
import com.loopers.application.payment.PaymentCallbackStatus
import com.loopers.application.payment.PaymentFacade
import com.loopers.infrastructure.payment.PgCallbackSignatureVerifier
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PaymentV1Controller")
class PaymentV1ControllerTest {
    private val paymentFacade: PaymentFacade = mockk(relaxed = true)
    private val verifier = PgCallbackSignatureVerifier("callback-secret")
    private val controller = PaymentV1Controller(paymentFacade, verifier)

    @DisplayName("유효한 서명의 승인 콜백이면 후처리를 위임한다")
    @Test
    fun delegatesCallback_whenSignatureIsValid() {
        val request = PaymentV1Dto.PgCallbackRequest(
            orderId = 100L,
            transactionId = "pg-100",
            status = PaymentCallbackStatus.APPROVED,
            failureReason = null,
            signature = verifier.sign("100|APPROVED|pg-100|"),
        )

        val response = controller.handlePgCallback(request)

        assertThat(response.meta.result).isEqualTo(com.loopers.interfaces.api.ApiResponse.Metadata.Result.SUCCESS)
        verify(exactly = 1) {
            paymentFacade.handleCallback(
                PaymentCallbackCommand(
                    orderId = 100L,
                    transactionId = "pg-100",
                    status = PaymentCallbackStatus.APPROVED,
                    failureReason = null,
                ),
            )
        }
    }

    @DisplayName("유효하지 않은 서명이면 BAD_REQUEST 예외가 발생한다")
    @Test
    fun throwsBadRequest_whenSignatureIsInvalid() {
        val request = PaymentV1Dto.PgCallbackRequest(
            orderId = 100L,
            transactionId = "pg-100",
            status = PaymentCallbackStatus.APPROVED,
            failureReason = null,
            signature = "invalid-signature",
        )

        assertThatThrownBy {
            controller.handlePgCallback(request)
        }
            .isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
    }
}
