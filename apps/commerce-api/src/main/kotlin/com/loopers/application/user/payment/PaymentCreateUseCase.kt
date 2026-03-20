package com.loopers.application.user.payment

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentIdempotencyKey
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PgPaymentPort
import com.loopers.domain.payment.PgPaymentRequest
import com.loopers.domain.payment.PgPaymentResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.security.MessageDigest

@Service
class PaymentCreateUseCase(
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository,
    private val pgPaymentPort: PgPaymentPort,
    private val transactionTemplate: TransactionTemplate,
    @Value("\${pg.callback-base-url}") private val callbackBaseUrl: String,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun create(command: PaymentCreateCommand): PaymentCreateResult {
        if (!pgPaymentPort.isAvailable()) {
            throw CoreException(ErrorType.PG_CIRCUIT_OPEN)
        }

        val idempotencyKey = PaymentIdempotencyKey(command.idempotencyKey)
        val fingerprint = computeFingerprint(command.orderId, command.cardType, command.cardNo)

        val txResult = transactionTemplate.execute {
            createPaymentInTransaction(command, idempotencyKey, fingerprint)
        }!!

        if (txResult is TransactionResult.IdempotentReplay) {
            return PaymentCreateResult.IdempotentReplay(txResult.result)
        }

        val created = (txResult as TransactionResult.Created)
        val payment = created.payment
        val order = created.order

        val pgRequest = PgPaymentRequest(
            userId = command.userId,
            orderId = command.orderId,
            cardType = command.cardType,
            cardNo = command.cardNo,
            amount = payment.amount.amount,
            callbackUrl = "$callbackBaseUrl/webhook/v1/payments/${payment.id}",
        )

        val pgResponse = pgPaymentPort.requestPayment(pgRequest)

        val updatedPayment = when (pgResponse) {
            is PgPaymentResponse.Accepted -> payment.updateTransactionKey(pgResponse.transactionKey)
            is PgPaymentResponse.ImmediateFailure -> payment.fail(pgResponse.reasonCode)
            is PgPaymentResponse.Timeout -> payment.applyTimeoutFallback()
            is PgPaymentResponse.CircuitOpen -> payment
        }

        if (updatedPayment !== payment) {
            paymentRepository.save(updatedPayment)
        }

        return PaymentCreateResult.NewlyCreated(
            PaymentResult.Created.from(updatedPayment, order.status),
        )
    }

    private fun createPaymentInTransaction(
        command: PaymentCreateCommand,
        idempotencyKey: PaymentIdempotencyKey,
        fingerprint: String,
    ): TransactionResult {
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
        return TransactionResult.Created(saved, order)
    }

    private fun handleIdempotentRequest(
        existing: Payment,
        fingerprint: String,
    ): TransactionResult {
        if (existing.requestFingerprint == fingerprint) {
            val order = orderRepository.findById(existing.orderId)!!
            return TransactionResult.IdempotentReplay(
                PaymentResult.Created.from(existing, order.status),
            )
        }
        throw CoreException(ErrorType.PAYMENT_IDEMPOTENCY_CONFLICT)
    }

    private sealed interface TransactionResult {
        data class Created(val payment: Payment, val order: Order) : TransactionResult
        data class IdempotentReplay(val result: PaymentResult.Created) : TransactionResult
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
