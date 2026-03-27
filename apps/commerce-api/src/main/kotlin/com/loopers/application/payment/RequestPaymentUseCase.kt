package com.loopers.application.payment

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
            orderId = buildPgOrderId(command.orderId, paymentId),
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
                log.warn(
                    "PG 결제 요청 실패. paymentId={}, reason={}, PENDING 유지하고 복구 스케줄러에 위임합니다.",
                    paymentId,
                    pgResponse.reason,
                )
            }
        } catch (e: Exception) {
            log.warn("PG 결제 요청 중 예외 발생. paymentId={}, PENDING 유지하고 복구 스케줄러에 위임합니다.", paymentId, e)
        }
    }

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")

        fun buildPgOrderId(orderId: Long, paymentId: Long): String {
            val date = LocalDate.now().format(DATE_FORMATTER)
            return "${date}_${orderId}_$paymentId"
        }
    }
}
