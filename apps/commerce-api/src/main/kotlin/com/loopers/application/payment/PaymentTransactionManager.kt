package com.loopers.application.payment

import com.loopers.application.outbox.OutboxEventWriter
import com.loopers.domain.coupon.UserCouponRepository
import com.loopers.domain.event.PaymentApprovedEvent
import com.loopers.domain.event.PaymentFailedEvent
import com.loopers.domain.order.OrderItemRepository
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.outbox.OutboxEventType
import com.loopers.domain.product.Money
import com.loopers.domain.payment.Payment
import org.slf4j.LoggerFactory
import com.loopers.domain.payment.PaymentHistory
import com.loopers.domain.payment.PaymentHistoryRepository
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PaymentStatus
import com.loopers.domain.product.ProductStockRepository
import com.loopers.support.error.PaymentException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PaymentTransactionManager(
    private val paymentRepository: PaymentRepository,
    private val paymentHistoryRepository: PaymentHistoryRepository,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val productStockRepository: ProductStockRepository,
    private val userCouponRepository: UserCouponRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val outboxEventWriter: OutboxEventWriter,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun saveOrResetPayment(command: PaymentCommand.Request): Payment {
        val order = orderRepository.findByIdOrNull(command.orderId)
            ?: throw PaymentException.orderNotFound()

        val existing = paymentRepository.findByOrderId(order.id)

        if (existing != null) {
            return when (existing.status) {
                PaymentStatus.APPROVED -> throw PaymentException.alreadyPaid()
                PaymentStatus.REQUESTED -> throw PaymentException.alreadyInProgress()
                PaymentStatus.FAILED,
                PaymentStatus.REQUEST_FAILED,
                -> {
                    paymentHistoryRepository.save(PaymentHistory.from(existing))
                    existing.resetForRetry()
                    if (order.status == OrderStatus.CANCELLED) {
                        order.reorder()
                        log.info("결제 재시도로 주문 복원 [orderId={}]", order.id)
                    }
                    order.userCouponId?.let { userCouponId ->
                        val reused = userCouponRepository.markAsUsed(userCouponId, order.id)
                        if (reused) {
                            log.info("결제 재시도로 쿠폰 재사용 [userCouponId={}, orderId={}]", userCouponId, order.id)
                        }
                    }
                    existing
                }
            }
        }

        val payment = Payment.create(
            orderId = order.id,
            userId = command.userId,
            amount = Money(order.totalAmount.amount),
            cardType = command.cardType,
            cardNo = command.cardNo,
        )
        return paymentRepository.save(payment)
    }

    @Transactional
    fun updateTransactionId(paymentId: Long, transactionId: String): PaymentInfo {
        val payment = paymentRepository.findByIdOrNull(paymentId)
            ?: throw PaymentException.notFound()
        payment.updateTransactionId(transactionId)
        return PaymentInfo.from(payment)
    }

    @Transactional
    fun markRequestFailed(paymentId: Long, reason: String?): PaymentInfo {
        val payment = paymentRepository.findByIdOrNull(paymentId)
            ?: throw PaymentException.notFound()
        payment.markRequestFailed(reason)
        return PaymentInfo.from(payment)
    }

    @Transactional
    fun applyPgResult(paymentId: Long, pgStatus: String, reason: String?, transactionId: String? = null): PaymentInfo {
        if (transactionId != null) {
            paymentRepository.updateTransactionIdIfAbsent(paymentId, transactionId)
        }

        when (pgStatus) {
            PG_STATUS_SUCCESS -> {
                val updatedCount = paymentRepository.approveIfNotTerminal(paymentId)
                if (updatedCount > 0) {
                    val productIds = deductStock(paymentId)
                    val payment = paymentRepository.findByIdOrNull(paymentId)!!
                    val event = PaymentApprovedEvent(
                        paymentId = paymentId,
                        orderId = payment.orderId,
                        userId = payment.userId,
                        amount = payment.amount.amount,
                        productIds = productIds,
                    )
                    eventPublisher.publishEvent(event)
                    outboxEventWriter.write(
                        eventType = OutboxEventType.PAYMENT_APPROVED,
                        partitionKey = payment.orderId.toString(),
                        payload = event,
                    )
                }
            }
            PG_STATUS_PENDING, PG_STATUS_REQUESTED -> { }
            else -> {
                val updatedCount = paymentRepository.failIfNotTerminal(paymentId, reason ?: "PG 결제 실패")
                if (updatedCount > 0) {
                    cancelOrderAndRestoreCoupon(paymentId)
                    val payment = paymentRepository.findByIdOrNull(paymentId)!!
                    val event = PaymentFailedEvent(
                        paymentId = paymentId,
                        orderId = payment.orderId,
                        userId = payment.userId,
                        reason = reason ?: "PG 결제 실패",
                    )
                    eventPublisher.publishEvent(event)
                    outboxEventWriter.write(
                        eventType = OutboxEventType.PAYMENT_FAILED,
                        partitionKey = payment.orderId.toString(),
                        payload = event,
                    )
                }
            }
        }

        val payment = paymentRepository.findByIdOrNull(paymentId)
            ?: throw PaymentException.notFound()
        return PaymentInfo.from(payment)
    }

    private fun deductStock(paymentId: Long): List<Long> {
        val payment = paymentRepository.findByIdOrNull(paymentId) ?: return emptyList()
        val orderItems = orderItemRepository.findByOrderId(payment.orderId)

        val productIds = orderItems.map { it.productId }.distinct().sorted()
        productIds.forEach { productId ->
            val productStock = productStockRepository.findByProductIdForUpdate(productId) ?: return@forEach
            val totalQuantity = orderItems.filter { it.productId == productId }.sumOf { it.quantity.value }
            productStock.decreaseStock(totalQuantity)
        }

        log.info("결제 승인으로 재고 차감 [paymentId={}, orderId={}]", paymentId, payment.orderId)
        return productIds
    }

    private fun cancelOrderAndRestoreCoupon(paymentId: Long) {
        val payment = paymentRepository.findByIdOrNull(paymentId) ?: return
        val order = orderRepository.findByIdOrNull(payment.orderId) ?: return
        order.cancel()
        log.info("결제 실패로 주문 취소 [paymentId={}, orderId={}]", paymentId, payment.orderId)

        order.userCouponId?.let { userCouponId ->
            val restored = userCouponRepository.restoreIfUsed(userCouponId)
            if (restored) {
                log.info("결제 실패로 쿠폰 반환 [userCouponId={}, orderId={}]", userCouponId, order.id)
            }
        }
    }

    companion object {
        private const val PG_STATUS_SUCCESS = "SUCCESS"
        private const val PG_STATUS_PENDING = "PENDING"
        private const val PG_STATUS_REQUESTED = "REQUESTED"
    }
}
