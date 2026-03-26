package com.loopers.application.payment

import com.loopers.application.product.ProductCacheStore
import com.loopers.domain.coupon.UserCouponRepository
import com.loopers.domain.order.OrderItemRepository
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.product.ProductStockRepository
import com.loopers.support.error.PaymentException
import com.loopers.support.transaction.AfterCommit
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class HandlePaymentCallbackUseCase(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val productStockRepository: ProductStockRepository,
    private val userCouponRepository: UserCouponRepository,
    private val productCacheStore: ProductCacheStore,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val PG_STATUS_SUCCESS = "SUCCESS"
    }

    @Transactional
    fun execute(command: PaymentCommand.Callback): PaymentInfo {
        val payment = paymentRepository.findByTransactionId(command.transactionId)
            ?: throw PaymentException.notFound()

        if (payment.status.isTerminal()) {
            log.info(
                "이미 처리된 결제 콜백 무시 [transactionId={}, status={}]",
                command.transactionId,
                payment.status,
            )
            return PaymentInfo.from(payment)
        }

        val updatedCount = if (command.status == PG_STATUS_SUCCESS) {
            paymentRepository.approveIfNotTerminal(payment.id)
        } else {
            paymentRepository.failIfNotTerminal(payment.id, command.reason ?: "PG 결제 실패")
        }

        if (updatedCount == 0) {
            log.info(
                "동시 콜백 처리로 이미 반영됨 [transactionId={}, paymentId={}]",
                command.transactionId,
                payment.id,
            )
        }

        if (updatedCount > 0 && command.status == PG_STATUS_SUCCESS) {
            deductStock(payment.orderId)
        }

        if (updatedCount > 0 && command.status != PG_STATUS_SUCCESS) {
            cancelOrderAndRestoreCoupon(payment.orderId)
        }

        val updated = paymentRepository.findByIdOrNull(payment.id)
            ?: throw PaymentException.notFound()
        return PaymentInfo.from(updated)
    }

    private fun deductStock(orderId: Long) {
        val orderItems = orderItemRepository.findByOrderId(orderId)

        val productIds = orderItems.map { it.productId }.distinct().sorted()
        productIds.forEach { productId ->
            val productStock = productStockRepository.findByProductIdForUpdate(productId) ?: return@forEach
            val totalQuantity = orderItems.filter { it.productId == productId }.sumOf { it.quantity.value }
            productStock.decreaseStock(totalQuantity)
        }

        log.info("결제 승인으로 재고 차감 [orderId={}]", orderId)

        AfterCommit.execute {
            productIds.forEach { productCacheStore.evictDetail(it) }
        }
    }

    private fun cancelOrderAndRestoreCoupon(orderId: Long) {
        val order = orderRepository.findByIdOrNull(orderId) ?: return
        order.cancel()
        log.info("결제 실패로 주문 취소 [orderId={}]", orderId)

        order.userCouponId?.let { userCouponId ->
            val restored = userCouponRepository.restoreIfUsed(userCouponId)
            if (restored) {
                log.info("결제 실패로 쿠폰 반환 [userCouponId={}, orderId={}]", userCouponId, orderId)
            }
        }
    }
}
