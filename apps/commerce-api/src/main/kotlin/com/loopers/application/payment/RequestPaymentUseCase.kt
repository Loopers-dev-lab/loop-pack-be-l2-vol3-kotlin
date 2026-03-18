package com.loopers.application.payment

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RequestPaymentUseCase(
    private val paymentTransactionService: PaymentTransactionService,
    private val paymentGatewayPort: PaymentGatewayPort,
    private val callbackUrlProvider: CallbackUrlProvider,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun request(userId: Long, command: RequestPaymentCommand): Long {
        val result = paymentTransactionService.createPendingPayment(userId, command)
        callPgAndUpdateStatus(result.paymentId, result.amount, userId, command)
        return result.paymentId
    }

    private fun callPgAndUpdateStatus(paymentId: Long, amount: Long, userId: Long, command: RequestPaymentCommand) {
        val pgRequest = PgPaymentRequest(
            orderId = command.orderId.toString().padStart(6, '0'),
            cardType = command.cardType,
            cardNo = command.cardNo,
            amount = amount,
            callbackUrl = callbackUrlProvider.getCallbackUrl(),
            userId = userId.toString(),
        )

        try {
            val pgResponse = paymentGatewayPort.requestPayment(pgRequest)

            if (pgResponse.success && pgResponse.transactionKey != null) {
                paymentTransactionService.markRequested(paymentId, pgResponse.transactionKey)
            } else {
                paymentTransactionService.markFailed(paymentId, pgResponse.reason ?: "PG 결제 요청 실패")
            }
        } catch (e: Exception) {
            log.warn("PG 결제 요청 중 예외 발생. paymentId={}, 콜백 또는 복구로 처리 예정", paymentId, e)
        }
    }
}
