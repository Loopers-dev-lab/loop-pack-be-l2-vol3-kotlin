package com.loopers.application.payment

import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.PaymentGateway
import com.loopers.domain.payment.PaymentService
import com.loopers.domain.payment.PaymentStatus
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PaymentFacade(
    private val paymentService: PaymentService,
    private val paymentGateway: PaymentGateway,
    @Value("\${pg.callback-url:http://localhost:8080/api/v1/payments/callback}") private val callbackUrl: String,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun requestPayment(
        userId: Long,
        orderId: String,
        cardType: CardType,
        cardNo: String,
        amount: Long,
    ): PaymentInfo {
        val payment = paymentService.createPayment(userId, orderId, cardType, cardNo, amount)

        try {
            val pgResponse = paymentGateway.requestPayment(
                userId = userId.toString(),
                orderId = orderId,
                cardType = cardType.name,
                cardNo = cardNo,
                amount = amount,
                callbackUrl = callbackUrl,
            )
            paymentService.markPending(payment.id, pgResponse.transactionKey)
            return PaymentInfo.from(paymentService.getPayment(payment.id))
        } catch (e: Exception) {
            log.warn("PG 호출 실패, Fallback 처리 (orderId={}): {}", orderId, e.message)
            return PaymentInfo.from(payment)
        }
    }

    @Transactional
    fun handleCallback(transactionKey: String, status: String, reason: String?) {
        val payment = paymentService.getPaymentByTransactionKey(transactionKey)
        when (status) {
            "SUCCESS" -> paymentService.markSuccess(payment.id)
            "FAILED" -> paymentService.markFailed(payment.id, reason ?: "알 수 없는 사유")
        }
    }

    @Transactional
    fun syncPaymentStatus(orderId: String): List<PaymentInfo> {
        val payments = paymentService.getPaymentsByOrderId(orderId)

        return payments.map { payment ->
            if (payment.status != PaymentStatus.PENDING && payment.status != PaymentStatus.REQUESTED) {
                return@map PaymentInfo.from(payment)
            }

            val transactionKey = payment.transactionKey ?: return@map PaymentInfo.from(payment)

            try {
                val pgDetail = paymentGateway.getTransactionStatus(payment.userId.toString(), transactionKey)
                when (pgDetail.status) {
                    "SUCCESS" -> paymentService.markSuccess(payment.id)
                    "FAILED" -> paymentService.markFailed(payment.id, pgDetail.reason ?: "알 수 없는 사유")
                }
                PaymentInfo.from(paymentService.getPayment(payment.id))
            } catch (e: Exception) {
                log.warn("PG 상태 조회 실패 (transactionKey={}): {}", transactionKey, e.message)
                PaymentInfo.from(payment)
            }
        }
    }

    @Transactional(readOnly = true)
    fun getPaymentsByOrderId(orderId: String): List<PaymentInfo> {
        return paymentService.getPaymentsByOrderId(orderId).map { PaymentInfo.from(it) }
    }
}
