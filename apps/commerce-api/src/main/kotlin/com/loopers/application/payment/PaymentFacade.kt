package com.loopers.application.payment

import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.PaymentGateway
import com.loopers.domain.payment.PaymentService
import com.loopers.domain.payment.PaymentStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PaymentFacade(
    private val paymentService: PaymentService,
    private val paymentGateway: PaymentGateway,
    @Value("\${pg.callback-url:http://localhost:8080/api/v1/payments/callback}") private val callbackUrl: String,
) {

    @Transactional
    fun requestPayment(
        userId: Long,
        orderId: String,
        cardType: CardType,
        cardNo: String,
        amount: Long,
    ): PaymentInfo {
        // 1. 결제를 REQUESTED 상태로 저장
        val payment = paymentService.createPayment(userId, orderId, cardType, cardNo, amount)

        // 2. PG 호출 — 응답이 없으면 REQUESTED 유지 (Fallback)
        val pgResponse = paymentGateway.requestPayment(
            userId = userId.toString(),
            orderId = orderId,
            cardType = cardType.name,
            cardNo = cardNo,
            amount = amount,
            callbackUrl = callbackUrl,
        ) ?: return PaymentInfo.from(payment)

        // 3. PENDING 상태로 전환
        paymentService.markPending(payment.id, pgResponse.transactionKey)
        return PaymentInfo.from(paymentService.getPayment(payment.id))
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

            val pgDetail = paymentGateway.getTransactionStatus(payment.userId.toString(), transactionKey)
                ?: return@map PaymentInfo.from(payment)

            when (pgDetail.status) {
                "SUCCESS" -> paymentService.markSuccess(payment.id)
                "FAILED" -> paymentService.markFailed(payment.id, pgDetail.reason ?: "알 수 없는 사유")
                else -> return@map PaymentInfo.from(payment)
            }
            PaymentInfo.from(paymentService.getPayment(payment.id))
        }
    }

    @Transactional(readOnly = true)
    fun getPaymentsByOrderId(orderId: String): List<PaymentInfo> {
        return paymentService.getPaymentsByOrderId(orderId).map { PaymentInfo.from(it) }
    }
}
