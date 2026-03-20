package com.loopers.infrastructure.payment

import com.loopers.domain.BaseEntity
import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.PaymentModel
import com.loopers.domain.payment.PaymentStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "payments")
class PaymentJpaModel(
    orderId: Long,
    memberId: Long,
    cardType: CardType,
    cardNo: String,
    amount: Long,
) : BaseEntity() {
    @Column(name = "order_id", nullable = false)
    var orderId: Long = orderId
        protected set

    @Column(name = "member_id", nullable = false)
    var memberId: Long = memberId
        protected set

    @Column(name = "transaction_key", length = 100)
    var transactionKey: String? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false, length = 20)
    var cardType: CardType = cardType
        protected set

    @Column(name = "card_no", nullable = false, length = 19)
    var cardNo: String = cardNo
        protected set

    @Column(name = "amount", nullable = false)
    var amount: Long = amount
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: PaymentStatus = PaymentStatus.REQUESTED
        protected set

    @Column(name = "fail_reason", length = 500)
    var failReason: String? = null
        protected set

    @Column(name = "requested_at", nullable = false)
    var requestedAt: ZonedDateTime = ZonedDateTime.now()
        protected set

    @Column(name = "completed_at")
    var completedAt: ZonedDateTime? = null
        protected set

    fun updateFrom(model: PaymentModel) {
        this.transactionKey = model.transactionKey
        this.status = model.status
        this.failReason = model.failReason
        this.completedAt = model.completedAt
    }

    fun toModel(): PaymentModel = PaymentModel(
        id = id,
        orderId = orderId,
        memberId = memberId,
        transactionKey = transactionKey,
        cardType = cardType,
        cardNo = cardNo,
        amount = amount,
        status = status,
        failReason = failReason,
        requestedAt = requestedAt,
        completedAt = completedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )

    companion object {
        fun from(model: PaymentModel): PaymentJpaModel {
            val jpaModel = PaymentJpaModel(
                orderId = model.orderId,
                memberId = model.memberId,
                cardType = model.cardType,
                cardNo = model.cardNo,
                amount = model.amount,
            )
            jpaModel.transactionKey = model.transactionKey
            jpaModel.status = model.status
            jpaModel.failReason = model.failReason
            jpaModel.requestedAt = model.requestedAt
            jpaModel.completedAt = model.completedAt
            return jpaModel
        }
    }
}
