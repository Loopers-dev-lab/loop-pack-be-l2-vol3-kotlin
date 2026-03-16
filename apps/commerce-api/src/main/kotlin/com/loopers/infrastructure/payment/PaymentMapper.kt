package com.loopers.infrastructure.payment

import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentStatus
import org.springframework.stereotype.Component

@Component
class PaymentMapper {

    fun toDomain(entity: PaymentEntity): Payment = Payment(
        id = entity.id,
        orderId = entity.orderId,
        memberId = entity.memberId,
        cardType = CardType.valueOf(entity.cardType),
        cardNo = entity.cardNo,
        amount = entity.amount,
        requestedAt = entity.requestedAt,
        status = PaymentStatus.valueOf(entity.status),
        pgTransactionKey = entity.pgTransactionKey,
        reason = entity.reason,
    )

    fun toEntity(domain: Payment): PaymentEntity = PaymentEntity(
        orderId = domain.orderId,
        memberId = domain.memberId,
        cardType = domain.cardType.name,
        cardNo = domain.cardNo,
        amount = domain.amount,
        status = domain.status.name,
        pgTransactionKey = domain.pgTransactionKey,
        reason = domain.reason,
        requestedAt = domain.requestedAt,
    )

    fun update(entity: PaymentEntity, domain: Payment) {
        entity.status = domain.status.name
        entity.pgTransactionKey = domain.pgTransactionKey
        entity.reason = domain.reason
    }
}
