package com.loopers.application.payment

import com.loopers.domain.order.Order
import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.Payment

class PaymentInfo {

    data class Detail(
        val paymentId: Long,
        val orderId: Long,
        val orderStatus: String,
        val paymentStatus: String,
        val amount: Long,
        val cardType: CardType,
        val cardNo: String,
        val transactionKey: String?,
        val reason: String?,
    ) {
        companion object {
            fun from(order: Order, payment: Payment) = Detail(
                paymentId = requireNotNull(payment.id) { "결제 저장 후 ID가 할당되지 않았습니다." },
                orderId = requireNotNull(order.id) { "주문 저장 후 ID가 할당되지 않았습니다." },
                orderStatus = order.status.name,
                paymentStatus = payment.status.name,
                amount = payment.amount,
                cardType = payment.cardType,
                cardNo = maskCardNo(payment.cardNo),
                transactionKey = payment.pgTransactionKey,
                reason = payment.reason,
            )

            private fun maskCardNo(cardNo: String): String {
                val digits = cardNo.filter(Char::isDigit)
                if (digits.isEmpty()) {
                    return "****"
                }
                return "****-****-****-${digits.takeLast(4)}"
            }
        }
    }
}
