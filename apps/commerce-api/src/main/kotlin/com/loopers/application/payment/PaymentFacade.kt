package com.loopers.application.payment

import com.loopers.domain.coupon.CouponService
import com.loopers.domain.order.OrderService
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.PaymentGateway
import com.loopers.domain.payment.PaymentService
import com.loopers.domain.payment.PaymentStatus
import com.loopers.domain.product.ProductService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PaymentFacade(
    private val paymentService: PaymentService,
    private val paymentGateway: PaymentGateway,
    private val orderService: OrderService,
    private val productService: ProductService,
    private val couponService: CouponService,
    @Value("\${pg.callback-url:http://localhost:8080/api/v1/payments/callback}") private val callbackUrl: String,
) {

    companion object {
        private const val PG_STATUS_SUCCESS = "SUCCESS"
        private const val PG_STATUS_FAILED = "FAILED"
        private const val DEFAULT_FAIL_REASON = "알 수 없는 사유"
        private val SYNCABLE_STATUSES = setOf(PaymentStatus.PENDING, PaymentStatus.REQUESTED)
    }

    @Transactional
    fun requestPayment(
        userId: Long,
        orderId: String,
        cardType: CardType,
        cardNo: String,
        amount: Long,
    ): PaymentInfo {
        // 1. 결제를 REQUESTED 상태로 저장
        val payment = paymentService.createPayment(userId, orderId, cardType, cardNo, amount)

        // 2. PG 호출
        val pgResponse = paymentGateway.requestPayment(
            userId = userId.toString(),
            orderId = orderId,
            cardType = cardType.name,
            cardNo = cardNo,
            amount = amount,
            callbackUrl = callbackUrl,
        )

        // 3. PG 즉시 FAILED 응답 처리 (카드 한도초과 등)
        if (pgResponse?.status == PG_STATUS_FAILED) {
            paymentService.markFailed(payment.id, pgResponse.reason ?: DEFAULT_FAIL_REASON)
            return PaymentInfo.from(paymentService.getPayment(payment.id))
        }

        // 4. PG 응답 또는 복구 조회를 통해 transactionKey 확보
        val transactionKey = pgResponse?.transactionKey
            ?: recoverTransactionKey(userId.toString(), orderId, payment.id)

        if (transactionKey != null) {
            paymentService.markPending(payment.id, transactionKey)
        } else {
            paymentService.markFailed(payment.id, "PG 결제 요청에 실패했습니다. 다시 시도해주세요.")
        }

        return PaymentInfo.from(paymentService.getPayment(payment.id))
    }

    @Transactional
    fun handleCallback(transactionKey: String, status: String, reason: String?) {
        val payment = paymentService.getPaymentByTransactionKey(transactionKey)

        // 콜백 데이터를 그대로 신뢰하지 않고, PG에 실제 상태를 조회하여 확인
        val pgDetail = paymentGateway.getTransactionStatus(payment.userId.toString(), transactionKey)

        // PG 재조회 실패 시 검증 불가 — 상태를 변경하지 않고 PENDING 유지 (sync API로 복구)
        if (pgDetail == null) return

        val orderId = payment.orderId.toLong()

        when (pgDetail.status) {
            PG_STATUS_SUCCESS -> {
                paymentService.markSuccess(payment.id)
                orderService.changeStatus(orderId, OrderStatus.CONFIRMED)
            }
            PG_STATUS_FAILED -> {
                paymentService.markFailed(payment.id, pgDetail.reason ?: DEFAULT_FAIL_REASON)
                cancelOrderWithCompensation(orderId)
            }
        }
    }

    /**
     * PG 호출 실패 시 복구 조회: 같은 orderId의 과거 거래를 제외하고 현재 요청에 해당하는 거래만 선택
     */
    private fun recoverTransactionKey(userId: String, orderId: String, currentPaymentId: Long): String? {
        val pgTransactions = paymentGateway.getTransactionsByOrderId(userId, orderId)
        if (pgTransactions.isEmpty()) return null

        // DB에 이미 매핑된 transactionKey를 제외하여 과거 거래 재사용 방지
        val existingKeys = paymentService.getPaymentsByOrderId(orderId)
            .filter { it.id != currentPaymentId }
            .mapNotNull { it.transactionKey }
            .toSet()

        return pgTransactions
            .filter { it.transactionKey !in existingKeys }
            .firstOrNull()?.transactionKey
    }

    private fun cancelOrderWithCompensation(orderId: Long) {
        val order = orderService.getOrderById(orderId)
        orderService.changeStatus(orderId, OrderStatus.CANCELLED)

        // 재고 복구
        val products = productService.getProductsByIds(order.items.map { it.productId })
        val productMap = products.associateBy { it.id }
        order.items.forEach { item ->
            productMap.getValue(item.productId).restoreStock(item.quantity)
        }

        // 쿠폰 복구
        order.couponId?.let { couponId ->
            couponService.restoreIssuedCoupon(couponId, order.userId)
        }
    }

    @Transactional
    fun syncPaymentStatus(orderId: String): List<PaymentInfo> {
        val payments = paymentService.getPaymentsByOrderId(orderId)

        return payments.map { payment ->
            if (payment.status !in SYNCABLE_STATUSES) {
                return@map PaymentInfo.from(payment)
            }

            val transactionKey = payment.transactionKey ?: return@map PaymentInfo.from(payment)

            val pgDetail = paymentGateway.getTransactionStatus(payment.userId.toString(), transactionKey)
                ?: return@map PaymentInfo.from(payment)

            when (pgDetail.status) {
                PG_STATUS_SUCCESS -> paymentService.markSuccess(payment.id)
                PG_STATUS_FAILED -> paymentService.markFailed(payment.id, pgDetail.reason ?: DEFAULT_FAIL_REASON)
                else -> return@map PaymentInfo.from(payment)
            }
            PaymentInfo.from(paymentService.getPayment(payment.id))
        }
    }

    @Transactional(readOnly = true)
    fun getPaymentsByOrderId(orderId: String): List<PaymentInfo> {
        return paymentService.getPaymentsByOrderId(orderId).map { PaymentInfo.from(it) }
    }
}
