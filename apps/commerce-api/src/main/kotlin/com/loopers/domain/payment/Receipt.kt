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
@Table(name = "receipts")
class Receipt protected constructor(
    val orderId: Long,
    val transactionId: String,
    val amount: BigDecimal,
    val cardType: String = "",
    val cardNo: String = "",
) : BaseEntity() {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ReceiptStatus = ReceiptStatus.PENDING
        protected set

    fun markAsCompleted(confirmedAmount: BigDecimal) {
        val allowedStatuses = listOf(ReceiptStatus.PENDING, ReceiptStatus.FAILED, ReceiptStatus.TIMEOUT)
        if (status !in allowedStatuses) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 완료는 PENDING, FAILED, TIMEOUT 상태에서만 가능합니다. 현재 상태: $status")
        }
        // PENDING인 경우에만 금액 검증 (FAILED/TIMEOUT은 PG 확인 결과이므로 신뢰)
        if (status == ReceiptStatus.PENDING && amount != confirmedAmount) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 금액이 일치하지 않습니다. 예상: $amount, 확인됨: $confirmedAmount")
        }
        this.status = ReceiptStatus.COMPLETED
    }

    fun markAsFailed() {
        if (status != ReceiptStatus.PENDING) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 상태가 올바르지 않습니다. 현재 상태: $status")
        }
        this.status = ReceiptStatus.FAILED
    }

    fun markAsCancelled() {
        if (status != ReceiptStatus.PENDING) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 상태가 올바르지 않습니다. 현재 상태: $status")
        }
        this.status = ReceiptStatus.CANCELLED
    }

    fun markAsTimeout() {
        if (status != ReceiptStatus.PENDING) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 상태가 올바르지 않습니다. 현재 상태: $status")
        }
        this.status = ReceiptStatus.TIMEOUT
    }

    companion object {
        fun create(
            orderId: Long,
            transactionId: String,
            amount: BigDecimal,
            cardType: String,
            cardNo: String,
        ): Receipt =
            Receipt(orderId, transactionId, amount, cardType, cardNo)
    }
}
