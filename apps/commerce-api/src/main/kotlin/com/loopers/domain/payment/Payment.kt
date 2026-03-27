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
import java.math.BigDecimal

@Entity
@Table(
    name = "payments",
    indexes = [
        Index(name = "idx_payments_order_id", columnList = "order_id"),
        Index(name = "idx_payments_transaction_key", columnList = "transaction_key"),
    ],
)
class Payment(
    orderId: Long,
    userId: Long,
    amount: BigDecimal,
    cardType: String,
    cardNo: String,
) : BaseEntity() {

    @Column(name = "order_id", nullable = false)
    val orderId: Long = orderId

    @Column(name = "user_id", nullable = false)
    val userId: Long = userId

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    val amount: BigDecimal = amount

    @Column(name = "card_type", nullable = false, length = 20)
    val cardType: String = cardType

    @Column(name = "card_no", nullable = false, length = 20)
    val cardNo: String = cardNo

    @Column(name = "transaction_key", length = 50)
    var transactionKey: String? = null
        private set

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: PaymentStatus = PaymentStatus.INITIATED
        private set

    @Column(name = "fail_reason", length = 200)
    var failReason: String? = null
        private set

    fun markRequested(transactionKey: String) {
        if (status != PaymentStatus.INITIATED) {
            throw CoreException(ErrorType.BAD_REQUEST, "INITIATED 상태에서만 PG 요청 완료 처리가 가능합니다.")
        }
        this.transactionKey = transactionKey
        status = PaymentStatus.REQUESTED
    }

    fun markPaid() {
        if (status != PaymentStatus.REQUESTED) {
            throw CoreException(ErrorType.BAD_REQUEST, "REQUESTED 상태에서만 결제 완료 처리가 가능합니다.")
        }
        status = PaymentStatus.PAID
    }

    fun markFailed(reason: String?) {
        if (status !in FAILABLE_STATUSES) {
            throw CoreException(ErrorType.BAD_REQUEST, "INITIATED 또는 REQUESTED 상태에서만 실패 처리가 가능합니다.")
        }
        status = PaymentStatus.FAILED
        failReason = reason
    }

    companion object {
        private val FAILABLE_STATUSES = setOf(PaymentStatus.INITIATED, PaymentStatus.REQUESTED)

        fun maskCardNo(cardNo: String): String {
            val parts = cardNo.split("-")
            return if (parts.size == 4) "****-****-****-${parts.last()}" else "****${cardNo.takeLast(4)}"
        }
    }
}
