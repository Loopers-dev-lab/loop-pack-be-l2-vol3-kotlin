package com.loopers.infrastructure.payment

import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.PaymentStatus
import com.loopers.support.jpa.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

@Entity
@Table(name = "payments")
class PaymentEntity(
    id: Long?,

    @Column(name = "order_id", nullable = false)
    val orderId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "transaction_key", length = 100)
    val transactionKey: String?,

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false, length = 20)
    val cardType: CardType,

    @Column(name = "card_no", nullable = false, length = 30)
    val cardNo: String,

    @Column(name = "amount", nullable = false)
    val amount: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: PaymentStatus,

    @Column(name = "failure_reason", length = 500)
    val failureReason: String?,
) : BaseEntity() {
    init {
        this.id = id
    }
}
