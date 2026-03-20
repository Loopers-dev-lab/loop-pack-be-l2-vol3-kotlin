package com.loopers.application.payment

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

data class RequestPaymentCriteria(
    val loginId: String,
    val orderId: Long,
    val cardType: String,
    val cardNo: String,
) {
    init {
        if (orderId <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 ID는 0보다 커야 합니다.")
        }
        if (cardType.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "카드 종류는 비어있을 수 없습니다.")
        }
        if (cardNo.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "카드 번호는 비어있을 수 없습니다.")
        }
    }
}

data class PaymentCallbackCriteria(
    val transactionKey: String,
    val status: String,
    val reason: String? = null,
)

data class SyncPaymentCriteria(
    val loginId: String,
    val paymentId: Long,
) {
    init {
        if (paymentId <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 ID는 0보다 커야 합니다.")
        }
    }
}
