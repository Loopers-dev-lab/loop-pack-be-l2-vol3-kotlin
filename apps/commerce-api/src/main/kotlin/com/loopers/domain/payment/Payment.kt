package com.loopers.domain.payment

import com.loopers.config.jpa.MoneyConverter
import com.loopers.domain.BaseEntity
import com.loopers.domain.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.annotations.Comment

@Entity
@Table(name = "payment")
@Comment("결제")
class Payment(
    orderId: Long,
    userId: Long,
    amount: Money,
    cardType: String,
    cardNo: String,
) : BaseEntity() {

    @Comment("주문 ID")
    @Column(name = "order_id", nullable = false)
    var orderId: Long = orderId
        protected set

    @Comment("결제 요청자")
    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Comment("PG 트랜잭션 키")
    @Column(name = "transaction_key", unique = true)
    var transactionKey: String? = null
        protected set

    @Comment("결제 금액")
    @Convert(converter = MoneyConverter::class)
    @Column(name = "amount", nullable = false)
    var amount: Money = amount
        protected set

    @Comment("카드 종류")
    @Column(name = "card_type", nullable = false)
    var cardType: String = cardType
        protected set

    @Comment("카드 번호")
    @Column(name = "card_no", nullable = false)
    var cardNo: String = cardNo
        protected set

    @Comment("결제 상태")
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    var paymentStatus: PaymentStatus = PaymentStatus.REQUESTED
        protected set

    @Comment("실패 사유")
    @Column(name = "fail_reason")
    var failReason: String? = null
        protected set

    init {
        if (orderId <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 주문입니다.")
        }
        if (userId <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 회원입니다.")
        }
        if (amount <= Money.ZERO) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 금액은 0보다 커야 합니다.")
        }
        if (cardType.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "카드 종류는 필수입니다.")
        }
        if (cardNo.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "카드 번호는 필수입니다.")
        }
    }

    fun isFinalized(): Boolean =
        this.paymentStatus == PaymentStatus.APPROVED || this.paymentStatus == PaymentStatus.REJECTED

    fun updateTransactionKey(transactionKey: String) {
        this.transactionKey = transactionKey
    }

    fun markApproved(transactionKey: String) {
        if (this.paymentStatus == PaymentStatus.APPROVED) {
            return // 멱등성: 이미 승인된 건은 무시
        }
        validateStatusTransition(PaymentStatus.APPROVED)
        this.transactionKey = transactionKey
        this.paymentStatus = PaymentStatus.APPROVED
    }

    fun markRejected(failReason: String) {
        validateStatusTransition(PaymentStatus.REJECTED)
        this.paymentStatus = PaymentStatus.REJECTED
        this.failReason = failReason
    }

    fun markTimeout() {
        validateStatusTransition(PaymentStatus.TIMEOUT)
        this.paymentStatus = PaymentStatus.TIMEOUT
    }

    private fun validateStatusTransition(target: PaymentStatus) {
        val allowedTransitions = mapOf(
            PaymentStatus.REQUESTED to setOf(PaymentStatus.APPROVED, PaymentStatus.REJECTED, PaymentStatus.TIMEOUT),
            PaymentStatus.TIMEOUT to setOf(PaymentStatus.APPROVED, PaymentStatus.REJECTED),
        )
        val allowed = allowedTransitions[this.paymentStatus]
            ?: throw CoreException(ErrorType.BAD_REQUEST, "결제 상태 '${this.paymentStatus}'에서는 변경할 수 없습니다.")
        if (target !in allowed) {
            throw CoreException(
                ErrorType.BAD_REQUEST,
                "결제 상태 '${this.paymentStatus}'에서 '${target}'(으)로 변경할 수 없습니다.",
            )
        }
    }
}
