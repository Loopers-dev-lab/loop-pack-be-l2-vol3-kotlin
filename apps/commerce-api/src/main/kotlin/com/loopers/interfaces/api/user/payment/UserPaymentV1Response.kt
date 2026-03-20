package com.loopers.interfaces.api.user.payment

import com.loopers.application.user.payment.PaymentResult
import java.math.BigDecimal

class UserPaymentV1Response {

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
            fun from(result: PaymentResult.Created): Created = Created(
                paymentId = result.paymentId,
                orderId = result.orderId,
                status = result.status,
                transactionKey = result.transactionKey,
                displayStatus = result.displayStatus,
                reasonCode = result.reasonCode,
                amount = result.amount,
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
            fun from(result: PaymentResult.Detail): Detail = Detail(
                paymentId = result.paymentId,
                orderId = result.orderId,
                status = result.status,
                transactionKey = result.transactionKey,
                displayStatus = result.displayStatus,
                reasonCode = result.reasonCode,
                amount = result.amount,
                cardType = result.cardType,
                maskedCardNo = result.maskedCardNo,
            )
        }
    }
}
