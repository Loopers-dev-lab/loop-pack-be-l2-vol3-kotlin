package com.loopers.infrastructure.payment

import com.loopers.domain.payment.Payment

object PaymentMapper {

    fun toDomain(entity: PaymentEntity): Payment {
        val id = requireNotNull(entity.id) {
            "PaymentEntity.id가 null입니다. 저장된 Entity만 Domain으로 변환 가능합니다."
        }
        return Payment.reconstitute(
            persistenceId = id,
            refOrderId = entity.orderId,
            refUserId = entity.userId,
            transactionKey = entity.transactionKey,
            cardType = entity.cardType,
            cardNo = entity.cardNo,
            amount = entity.amount,
            status = entity.status,
            failureReason = entity.failureReason,
        )
    }

    fun toEntity(domain: Payment): PaymentEntity {
        return PaymentEntity(
            id = domain.persistenceId,
            orderId = domain.refOrderId,
            userId = domain.refUserId,
            transactionKey = domain.transactionKey,
            cardType = domain.cardType,
            cardNo = domain.cardNo,
            amount = domain.amount,
            status = domain.status,
            failureReason = domain.failureReason,
        )
    }
}
