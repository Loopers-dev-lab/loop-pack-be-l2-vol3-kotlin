package com.loopers.application.user.payment

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentIdempotencyKey
import com.loopers.domain.payment.PaymentRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest

@Service
class PaymentCreateUseCase(
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository,
) {

    @Transactional
    fun create(command: PaymentCreateCommand): PaymentCreateResult {
        val idempotencyKey = PaymentIdempotencyKey(command.idempotencyKey)
        val fingerprint = computeFingerprint(command.orderId, command.cardType, command.cardNo)

        val existing = paymentRepository.findByIdempotencyKey(idempotencyKey)
        if (existing != null) {
            return handleIdempotentRequest(existing, fingerprint)
        }

        val order = orderRepository.findById(command.orderId)
            ?: throw CoreException(ErrorType.ORDER_NOT_FOUND)

        if (order.status != Order.Status.PENDING) {
            throw CoreException(ErrorType.PAYMENT_ORDER_NOT_PENDING)
        }

        val activePending = paymentRepository.findActiveByOrderId(command.orderId)
        if (activePending != null) {
            throw CoreException(ErrorType.PAYMENT_ACTIVE_PENDING_EXISTS)
        }

        val payment = Payment.create(
            orderId = command.orderId,
            userId = command.userId,
            idempotencyKey = idempotencyKey,
            cardType = command.cardType,
            maskedCardNo = maskCardNo(command.cardNo),
            amount = order.finalAmount(),
            requestFingerprint = fingerprint,
        )

        val saved = paymentRepository.save(payment)

        return PaymentCreateResult.NewlyCreated(
            PaymentResult.Created.from(saved, order.status),
        )
    }

    private fun handleIdempotentRequest(
        existing: Payment,
        fingerprint: String,
    ): PaymentCreateResult {
        if (existing.requestFingerprint == fingerprint) {
            val order = orderRepository.findById(existing.orderId)!!
            return PaymentCreateResult.IdempotentReplay(
                PaymentResult.Created.from(existing, order.status),
            )
        }
        throw CoreException(ErrorType.PAYMENT_IDEMPOTENCY_CONFLICT)
    }

    companion object {
        fun computeFingerprint(orderId: Long, cardType: String, cardNo: String): String {
            val input = "$orderId|$cardType|$cardNo"
            val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }

        fun maskCardNo(cardNo: String): String {
            if (cardNo.length <= 4) return "****"
            return "*".repeat(cardNo.length - 4) + cardNo.takeLast(4)
        }
    }
}

sealed interface PaymentCreateResult {
    val result: PaymentResult.Created

    data class NewlyCreated(override val result: PaymentResult.Created) : PaymentCreateResult
    data class IdempotentReplay(override val result: PaymentResult.Created) : PaymentCreateResult
}
