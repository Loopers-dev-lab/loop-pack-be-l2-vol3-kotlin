package com.loopers.infrastructure.payment

import com.loopers.domain.common.Money
import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentIdempotencyKey
import org.springframework.stereotype.Component

@Component
class PaymentMapper {

    fun toDomain(entity: PaymentEntity): Payment = Payment.retrieve(
        id = entity.id!!,
        orderId = entity.orderId,
        userId = entity.userId,
        idempotencyKey = PaymentIdempotencyKey(entity.idempotencyKey),
        status = entity.status,
        cardType = entity.cardType,
        maskedCardNo = entity.maskedCardNo,
        amount = Money(entity.amount),
        transactionKey = entity.transactionKey,
        reasonCode = entity.reasonCode,
        requestFingerprint = entity.requestFingerprint,
        createdAt = entity.createdAt,
    )

    fun toEntity(payment: Payment): PaymentEntity = PaymentEntity(
        id = payment.id,
        orderId = payment.orderId,
        userId = payment.userId,
        idempotencyKey = payment.idempotencyKey.value,
        status = payment.status,
        cardType = payment.cardType,
        maskedCardNo = payment.maskedCardNo,
        amount = payment.amount.amount,
        transactionKey = payment.transactionKey,
        reasonCode = payment.reasonCode,
        requestFingerprint = payment.requestFingerprint,
    )
}
