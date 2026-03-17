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
class Payment protected constructor(
    val orderId: Long,
    val transactionId: String,
    val amount: BigDecimal,
    val cardType: String = "",
    val cardNo: String = "",
) : BaseEntity() {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: PaymentStatus = PaymentStatus.INITIATED
        protected set

    fun markAsCompleted(confirmedAmount: BigDecimal) {
        if (status != PaymentStatus.INITIATED) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 상태가 올바르지 않습니다. 현재 상태: $status")
        }
        if (amount != confirmedAmount) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 금액이 일치하지 않습니다. 예상: $amount, 확인됨: $confirmedAmount")
        }
        this.status = PaymentStatus.COMPLETED
    }

    fun markAsFailed() {
        if (status != PaymentStatus.INITIATED) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 상태가 올바르지 않습니다. 현재 상태: $status")
        }
        this.status = PaymentStatus.FAILED
    }

    fun markAsCancelled() {
        if (status != PaymentStatus.INITIATED) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 상태가 올바르지 않습니다. 현재 상태: $status")
        }
        this.status = PaymentStatus.CANCELLED
    }

    companion object {
        fun create(
            orderId: Long,
            transactionId: String,
            amount: BigDecimal,
            cardType: String = "",
            cardNo: String = "",
        ): Payment =
            Payment(orderId, transactionId, amount, cardType, cardNo)
    }
}
