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
        if (loginId.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 비어있을 수 없습니다.")
        }
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
) {
    companion object {
        private val ALLOWED_STATUSES = setOf("SUCCESS", "FAILED")
    }

    init {
        if (transactionKey.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "트랜잭션 키는 비어있을 수 없습니다.")
        }
        if (status.uppercase() !in ALLOWED_STATUSES) {
            throw CoreException(ErrorType.BAD_REQUEST, "허용되지 않는 콜백 상태입니다: $status")
        }
    }
}

data class SyncPaymentCriteria(
    val loginId: String,
    val paymentId: Long,
) {
    init {
        if (loginId.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 비어있을 수 없습니다.")
        }
        if (paymentId <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 ID는 0보다 커야 합니다.")
        }
    }
}
