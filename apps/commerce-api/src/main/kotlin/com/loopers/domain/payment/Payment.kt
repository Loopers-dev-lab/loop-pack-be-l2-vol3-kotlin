package com.loopers.domain.payment

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "payments",
    indexes = [
        Index(name = "idx_payments_order_id", columnList = "order_id"),
        Index(name = "idx_payments_transaction_key", columnList = "transaction_key"),
    ],
)
class Payment(
    userId: Long,
    orderId: String,
    cardType: CardType,
    cardNo: String,
    amount: Long,
) : BaseEntity() {

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Column(name = "order_id", nullable = false)
    var orderId: String = orderId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false)
    var cardType: CardType = cardType
        protected set

    @Column(name = "card_no", nullable = false)
    var cardNo: String = cardNo
        protected set

    @Column(name = "amount", nullable = false)
    var amount: Long = amount
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: PaymentStatus = PaymentStatus.REQUESTED
        protected set

    @Column(name = "transaction_key")
    var transactionKey: String? = null
        protected set

    @Column(name = "fail_reason")
    var failReason: String? = null
        protected set

    init {
        if (orderId.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 ID는 비어있을 수 없습니다.")
        }
        if (cardNo.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "카드 번호는 비어있을 수 없습니다.")
        }
        if (amount <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 금액은 0보다 커야 합니다.")
        }
    }

    fun markPending(transactionKey: String) {
        changeStatus(PaymentStatus.PENDING)
        this.transactionKey = transactionKey
    }

    fun markSuccess() {
        changeStatus(PaymentStatus.SUCCESS)
    }

    fun markFailed(reason: String) {
        changeStatus(PaymentStatus.FAILED)
        this.failReason = reason
    }

    private fun changeStatus(next: PaymentStatus) {
        if (!status.canTransitionTo(next)) {
            throw CoreException(
                ErrorType.BAD_REQUEST,
                "${status.name}에서 ${next.name}(으)로 상태를 변경할 수 없습니다.",
            )
        }
        this.status = next
    }
}
