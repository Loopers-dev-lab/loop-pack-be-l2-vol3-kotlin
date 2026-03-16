package com.loopers.infrastructure.payment

import com.loopers.domain.BaseEntity
import com.loopers.domain.payment.model.CardType
import com.loopers.domain.payment.model.Payment
import com.loopers.domain.payment.model.PaymentStatus
import com.loopers.domain.withBaseFields
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

@Entity
@Table(name = "payments")
class PaymentEntity(
    @Column(name = "order_id", nullable = false)
    var orderId: Long,
    @Column(name = "transaction_key", unique = true)
    var transactionKey: String?,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: PaymentStatus,
    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false)
    var cardType: CardType,
    @Column(name = "card_no", nullable = false)
    var cardNo: String,
    @Column(name = "amount", nullable = false)
    var amount: Long,
    @Column(name = "reason")
    var reason: String?,
) : BaseEntity() {

    companion object {
        fun fromDomain(payment: Payment): PaymentEntity {
            return PaymentEntity(
                orderId = payment.orderId,
                transactionKey = payment.transactionKey,
                status = payment.status,
                cardType = payment.cardType,
                cardNo = payment.cardNo,
                amount = payment.amount,
                reason = payment.reason,
            ).withBaseFields(
                id = payment.id,
                createdAt = if (payment.id != 0L) payment.createdAt else null,
                updatedAt = if (payment.id != 0L) payment.updatedAt else null,
            )
        }
    }

    fun toDomain(): Payment = Payment.fromPersistence(
        id = id,
        orderId = orderId,
        transactionKey = transactionKey,
        status = status,
        cardType = cardType,
        cardNo = cardNo,
        amount = amount,
        reason = reason,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
