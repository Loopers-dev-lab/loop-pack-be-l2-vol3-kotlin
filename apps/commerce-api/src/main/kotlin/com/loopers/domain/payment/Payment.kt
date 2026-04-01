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
        Index(name = "idx_payments_order_id", columnList = "order_id, deleted_at"),
        Index(name = "idx_payments_transaction_key", columnList = "transaction_key", unique = true),
    ],
)
class Payment(
    @Column(name = "order_id", nullable = false)
    val orderId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false)
    val cardType: CardType,

    @Column(name = "card_no", nullable = false)
    val cardNo: String,

    @Column(name = "amount", nullable = false)
    val amount: Long,
) : BaseEntity() {

    @Column(name = "transaction_key")
    var transactionKey: String? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: PaymentStatus = PaymentStatus.PENDING
        protected set

    @Column(name = "reason")
    var reason: String? = null
        protected set

    fun assignTransactionKey(key: String) {
        if (transactionKey != null) {
            throw CoreException(ErrorType.BAD_REQUEST, "이미 트랜잭션 키가 할당되었습니다.")
        }
        transactionKey = key
    }

    fun complete(status: PaymentStatus, reason: String?) {
        if (this.status != PaymentStatus.PENDING) {
            throw CoreException(ErrorType.BAD_REQUEST, "이미 처리된 결제입니다.")
        }
        this.status = status
        this.reason = reason
    }

    override fun guard() {
        if (orderId <= 0) throw CoreException(ErrorType.BAD_REQUEST, "주문 ID는 0보다 커야 합니다.")
        if (amount <= 0) throw CoreException(ErrorType.BAD_REQUEST, "결제 금액은 0보다 커야 합니다.")
        if (cardNo.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "카드 번호는 필수입니다.")
    }

    companion object {
        fun maskCardNo(cardNo: String): String {
            val digits = cardNo.replace("-", "")
            if (digits.length < 4) return cardNo
            val last4 = digits.takeLast(4)
            return "xxxx-xxxx-xxxx-$last4"
        }
    }
}
