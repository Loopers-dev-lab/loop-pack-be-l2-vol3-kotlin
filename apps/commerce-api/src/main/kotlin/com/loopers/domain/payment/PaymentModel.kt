package com.loopers.domain.payment

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.ZonedDateTime

@Entity
@Table(
    name = "payments",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_payments_order_id", columnNames = ["order_id"]),
    ],
)
class PaymentModel(
    orderId: Long,
    amount: Long,
    expiresAt: ZonedDateTime,
) : BaseEntity() {

    @Column(name = "order_id", nullable = false)
    var orderId: Long = orderId
        protected set

    @Column(name = "amount", nullable = false)
    var amount: Long = amount
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: PaymentStatus = PaymentStatus.PENDING
        protected set

    @Column(name = "external_transaction_id", length = 100)
    var externalTransactionId: String? = null
        protected set

    @Column(name = "failure_reason", length = 500)
    var failureReason: String? = null
        protected set

    @Column(name = "expires_at", nullable = false)
    var expiresAt: ZonedDateTime = expiresAt
        protected set

    fun markSucceeded(externalTransactionId: String) {
        if (status != PaymentStatus.PENDING) {
            throw CoreException(ErrorType.BAD_REQUEST, "성공 처리할 수 없습니다. (현재 상태: $status)")
        }
        this.status = PaymentStatus.SUCCEEDED
        this.externalTransactionId = externalTransactionId
        this.failureReason = null
    }

    fun markFailed(failureReason: String) {
        if (status != PaymentStatus.PENDING) {
            throw CoreException(ErrorType.BAD_REQUEST, "실패 처리할 수 없습니다. (현재 상태: $status)")
        }
        this.status = PaymentStatus.FAILED
        this.failureReason = failureReason
        this.externalTransactionId = null
    }

    fun markExpired() {
        if (status != PaymentStatus.PENDING) {
            throw CoreException(ErrorType.BAD_REQUEST, "만료 처리할 수 없습니다. (현재 상태: $status)")
        }
        this.status = PaymentStatus.EXPIRED
    }
}
