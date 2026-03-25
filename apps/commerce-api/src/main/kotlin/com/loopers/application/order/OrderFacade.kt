package com.loopers.application.order

import com.loopers.application.outbox.OutboxPublisher
import com.loopers.application.payment.PaymentFacade
import com.loopers.domain.brand.BrandService
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.Discount
import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.order.OrderItemCommand
import com.loopers.domain.order.OrderService
import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.PaymentStatus
import com.loopers.domain.product.ProductService
import com.loopers.domain.product.StockDeductionRequest
import com.loopers.domain.user.event.ActionType
import com.loopers.domain.user.event.UserActionEvent
import com.loopers.event.payload.OrderCompletedPayload
import com.loopers.event.payload.OrderItemPayload
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class OrderFacade(
    private val orderService: OrderService,
    private val productService: ProductService,
    private val brandService: BrandService,
    private val couponService: CouponService,
    private val paymentFacade: PaymentFacade,
    private val eventPublisher: ApplicationEventPublisher,
    private val outboxPublisher: OutboxPublisher,
) {

    @Transactional(readOnly = true)
    fun getOrder(userId: Long, orderId: Long): OrderDetailInfo {
        val order = orderService.getOrder(userId, orderId)
        return OrderDetailInfo.from(order)
    }

    @Transactional(readOnly = true)
    fun getOrders(userId: Long, startAt: LocalDateTime, endAt: LocalDateTime): List<OrderInfo> {
        if (startAt.isAfter(endAt)) {
            throw CoreException(ErrorType.BAD_REQUEST, "시작일이 종료일보다 클 수 없습니다.")
        }
        val orders = orderService.getOrders(userId, startAt, endAt)
        return orders.map { OrderInfo.from(it) }
    }

    @Transactional
    fun placeOrder(
        userId: Long,
        items: List<OrderPlaceCommand>,
        couponId: Long? = null,
        idempotencyKey: String? = null,
        cardType: CardType,
        cardNo: String,
    ) {
        // 멱등성 키 중복 체크
        if (idempotencyKey != null && orderService.findByIdempotencyKey(idempotencyKey) != null) {
            return
        }

        // 쿠폰 검증 (fail-fast)
        val couponInfo = couponId?.let { id ->
            val issuedCoupon = couponService.findIssuedCouponWithLock(id, userId)
            val coupon = couponService.findCouponById(id)
            issuedCoupon.validateUsable(coupon.expiresAt)
            CouponApplyInfo(id, coupon.discount, issuedCoupon)
        }

        val productIds = items.map { it.productId }

        // DB 비관적 락(SELECT FOR UPDATE)으로 상품 조회 + 존재 검증
        val products = productService.getProductsForOrderWithLock(productIds)
        val productMap = products.associateBy { it.id }

        val brandMap = brandService.getBrandsByIds(
            products.map { it.brandId }.distinct(),
        ).associateBy { it.id }

        val deductionRequests = items.map { StockDeductionRequest(it.productId, it.quantity) }
        productService.deductStocks(productMap, deductionRequests)

        // cross-domain 스냅샷 조립은 application 레이어에 유지
        val orderItemCommands = items.map { item ->
            val product = productMap.getValue(item.productId)
            val brand = brandMap.getValue(product.brandId)
            OrderItemCommand(
                productId = item.productId,
                quantity = item.quantity,
                productName = product.name,
                productPrice = product.price,
                brandName = brand.name,
            )
        }

        val order = orderService.createOrder(userId, orderItemCommands, idempotencyKey)

        // 쿠폰 할인 적용
        couponInfo?.let {
            val discountAmount = it.discount.calculateDiscountAmount(order.totalAmount)
            order.applyCouponDiscount(it.couponId, discountAmount)
            it.issuedCoupon.use()
        }

        // 결제 요청 — FAILED면 예외 → 트랜잭션 롤백 (재고/쿠폰 자동 복원)
        val paymentInfo = paymentFacade.requestPayment(
            userId = userId,
            orderId = order.id.toString(),
            cardType = cardType,
            cardNo = cardNo,
            amount = order.paymentAmount.value,
        )

        if (paymentInfo.status == PaymentStatus.FAILED) {
            throw CoreException(ErrorType.INTERNAL_ERROR, paymentInfo.failReason ?: "결제에 실패했습니다.")
        }

        // Outbox INSERT (Kafka 발행용)
        outboxPublisher.publish(
            aggregateType = "ORDER",
            aggregateId = order.id.toString(),
            eventType = "ORDER_COMPLETED",
            version = System.currentTimeMillis(),
            payload = OrderCompletedPayload(
                orderId = order.id,
                userId = userId,
                items = items.map {
                    OrderItemPayload(it.productId, it.quantity.value, productMap[it.productId]?.name ?: "")
                },
                couponId = couponId,
                totalAmount = order.totalAmount.value,
                paymentAmount = order.paymentAmount.value,
            ),
        )

        // 유저 행동 로깅 (인프로세스)
        eventPublisher.publishEvent(
            UserActionEvent(userId = userId, actionType = ActionType.ORDER_PLACED, targetId = order.id),
        )
    }

    private data class CouponApplyInfo(
        val couponId: Long,
        val discount: Discount,
        val issuedCoupon: IssuedCoupon,
    )
}
