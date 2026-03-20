package com.loopers.application.user.payment

import com.loopers.domain.order.Order
import com.loopers.domain.payment.DisplayStatus
import com.loopers.domain.payment.Payment
import java.math.BigDecimal

class PaymentResult {

    data class Created(
        val paymentId: Long,
        val orderId: Long,
        val status: String,
        val transactionKey: String?,
        val displayStatus: String,
        val reasonCode: String?,
        val amount: BigDecimal,
    ) {
        companion object {
            fun from(payment: Payment, orderStatus: Order.Status): Created = Created(
                paymentId = payment.id!!,
                orderId = payment.orderId,
                status = payment.status.name,
                transactionKey = payment.transactionKey,
                displayStatus = DisplayStatus.of(orderStatus, payment.status).name,
                reasonCode = payment.reasonCode?.name,
                amount = payment.amount.amount,
            )
        }
    }

    data class Detail(
        val paymentId: Long,
        val orderId: Long,
        val status: String,
        val transactionKey: String?,
        val displayStatus: String,
        val reasonCode: String?,
        val amount: BigDecimal,
        val cardType: String,
        val maskedCardNo: String,
    ) {
        companion object {
            fun from(payment: Payment, orderStatus: Order.Status): Detail = Detail(
                paymentId = payment.id!!,
                orderId = payment.orderId,
                status = payment.status.name,
                transactionKey = payment.transactionKey,
                displayStatus = DisplayStatus.of(orderStatus, payment.status).name,
                reasonCode = payment.reasonCode?.name,
                amount = payment.amount.amount,
                cardType = payment.cardType,
                maskedCardNo = payment.maskedCardNo,
            )
        }
    }
}
