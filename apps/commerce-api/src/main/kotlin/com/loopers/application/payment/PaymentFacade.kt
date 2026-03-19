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

    companion object {
        private const val DEFAULT_FAIL_REASON = "알 수 없는 사유"
    }

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

        // 2. PG 호출
        val pgResponse = paymentGateway.requestPayment(
            userId = userId.toString(),
            orderId = orderId,
            cardType = cardType.name,
            cardNo = cardNo,
            amount = amount,
            callbackUrl = callbackUrl,
        )

        // 3. PG 응답 또는 조회를 통해 transactionKey 확보
        val transactionKey = pgResponse?.transactionKey
            ?: paymentGateway.getTransactionsByOrderId(userId.toString(), orderId)
                .firstOrNull()?.transactionKey

        if (transactionKey != null) {
            paymentService.markPending(payment.id, transactionKey)
        } else {
            paymentService.markFailed(payment.id, "PG 결제 요청에 실패했습니다. 다시 시도해주세요.")
        }

        return PaymentInfo.from(paymentService.getPayment(payment.id))
    }

    @Transactional
    fun handleCallback(transactionKey: String, status: String, reason: String?) {
        val payment = paymentService.getPaymentByTransactionKey(transactionKey)

        // 콜백 데이터를 그대로 신뢰하지 않고, PG에 실제 상태를 조회하여 확인
        val pgDetail = paymentGateway.getTransactionStatus(payment.userId.toString(), transactionKey)

        val verifiedStatus = pgDetail?.status ?: status
        val verifiedReason = pgDetail?.reason ?: reason

        when (verifiedStatus) {
            "SUCCESS" -> paymentService.markSuccess(payment.id)
            "FAILED" -> paymentService.markFailed(payment.id, verifiedReason ?: DEFAULT_FAIL_REASON)
        }
    }

    @Transactional
    fun syncPaymentStatus(orderId: String): List<PaymentInfo> {
        val payments = paymentService.getPaymentsByOrderId(orderId)

        return payments.map { payment ->
            val syncableStatuses = setOf(PaymentStatus.PENDING, PaymentStatus.REQUESTED)
            if (payment.status !in syncableStatuses) {
                return@map PaymentInfo.from(payment)
            }

            val transactionKey = payment.transactionKey ?: return@map PaymentInfo.from(payment)

            val pgDetail = paymentGateway.getTransactionStatus(payment.userId.toString(), transactionKey)
                ?: return@map PaymentInfo.from(payment)

            when (pgDetail.status) {
                "SUCCESS" -> paymentService.markSuccess(payment.id)
                "FAILED" -> paymentService.markFailed(payment.id, pgDetail.reason ?: DEFAULT_FAIL_REASON)
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
