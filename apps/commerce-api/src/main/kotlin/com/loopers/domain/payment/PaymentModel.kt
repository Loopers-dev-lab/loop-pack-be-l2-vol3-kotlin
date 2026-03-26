package com.loopers.domain.payment

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "payments")
class PaymentModel(
    orderId: Long,
    userId: Long,
    amount: BigDecimal,
    cardType: String,
    cardNo: String,
) : BaseEntity() {
    @Column(name = "order_id", nullable = false)
    var orderId: Long = orderId
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Column(nullable = false, precision = 12, scale = 2)
    var amount: BigDecimal = amount
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PaymentStatus = PaymentStatus.PENDING
        protected set

    @Column(name = "card_type", nullable = false)
    var cardType: String = cardType
        protected set

    @Column(name = "card_no", nullable = false)
    var cardNo: String = cardNo
        protected set

    @Column(name = "transaction_key")
    var transactionKey: String? = null
        protected set

    @Column(name = "fail_reason")
    var failReason: String? = null
        protected set

    init {
        validateAmount(amount)
        validateCardType(cardType)
        validateCardNo(cardNo)
    }

    fun updateTransactionKey(transactionKey: String) {
        this.transactionKey = transactionKey
    }

    fun markSuccess() {
        if (status != PaymentStatus.PENDING) {
            throw CoreException(ErrorType.BAD_REQUEST, "PENDING 상태에서만 결제 완료 처리할 수 있습니다.")
        }
        status = PaymentStatus.SUCCESS
    }

    fun markFailed(reason: String? = null) {
        if (status != PaymentStatus.PENDING) {
            throw CoreException(ErrorType.BAD_REQUEST, "PENDING 상태에서만 결제 실패 처리할 수 있습니다.")
        }
        status = PaymentStatus.FAILED
        failReason = reason
    }

    private fun validateAmount(amount: BigDecimal) {
        if (amount <= BigDecimal.ZERO) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 금액은 0보다 커야 합니다.")
        }
    }

    private fun validateCardType(cardType: String) {
        if (cardType.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "카드 종류는 비어있을 수 없습니다.")
        }
    }

    private fun validateCardNo(cardNo: String) {
        if (cardNo.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "카드 번호는 비어있을 수 없습니다.")
        }
    }
}
