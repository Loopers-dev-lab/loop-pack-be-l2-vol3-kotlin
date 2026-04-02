package com.loopers.application.order

import com.loopers.application.outbox.OutboxPublisher
import com.loopers.application.payment.PaymentFacade
import com.loopers.domain.brand.BrandService
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.Discount
import com.loopers.domain.order.CouponReservationRepository
import com.loopers.domain.order.OrderItemCommand
import com.loopers.domain.order.OrderService
import com.loopers.domain.order.StockReservationRepository
import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.PaymentStatus
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductService
import com.loopers.domain.queue.OrderQueueService
import com.loopers.domain.user.event.ActionType
import com.loopers.domain.user.event.UserActionEvent
import com.loopers.event.AggregateTypes
import com.loopers.event.EventTypes
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
    private val stockReservationRepository: StockReservationRepository,
    private val couponReservationRepository: CouponReservationRepository,
    private val orderQueueService: OrderQueueService,
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
        entryToken: String,
        cardType: CardType,
        cardNo: String,
    ) {
        orderQueueService.validateAndConsumeToken(userId, entryToken)

        if (idempotencyKey != null && orderService.findByIdempotencyKey(idempotencyKey) != null) {
            return
        }

        val productMap = resolveProducts(items)
        val reservedStocks = reserveStocks(items)
        val couponReserved = reserveCouponIfPresent(couponId, userId, reservedStocks)

        try {
            val couponInfo = validateCouponIfPresent(couponId, userId)

            val orderItemCommands = buildOrderItemCommands(items, productMap)
            val order = orderService.createOrder(userId, orderItemCommands, idempotencyKey)

            couponInfo?.let {
                val discountAmount = it.discount.calculateDiscountAmount(order.totalAmount)
                order.applyCouponDiscount(it.couponId, discountAmount)
            }

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

            publishOrderCompletedEvent(
                order.id,
                userId,
                items,
                productMap,
                couponId,
                order.totalAmount.value,
                order.paymentAmount.value,
            )

            eventPublisher.publishEvent(
                UserActionEvent(userId = userId, actionType = ActionType.ORDER_PLACED, targetId = order.id),
            )
        } catch (e: Exception) {
            restoreReservations(reservedStocks, couponId, userId, couponReserved)
            throw e
        }
    }

    private fun resolveProducts(items: List<OrderPlaceCommand>): Map<Long, Product> {
        val productIds = items.map { it.productId }
        val products = productService.getProductsByIds(productIds)
        val productMap = products.associateBy { it.id }

        val missingIds = productIds.filter { it !in productMap }
        if (missingIds.isNotEmpty()) {
            throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다: $missingIds")
        }
        return productMap
    }

    private fun reserveStocks(items: List<OrderPlaceCommand>): List<Pair<Long, Int>> {
        val reservedStocks = mutableListOf<Pair<Long, Int>>()
        try {
            items.forEach { item ->
                if (!stockReservationRepository.reserve(item.productId, item.quantity.value)) {
                    throw CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.")
                }
                reservedStocks.add(item.productId to item.quantity.value)
            }
        } catch (e: Exception) {
            reservedStocks.forEach { (productId, qty) -> stockReservationRepository.restore(productId, qty) }
            throw e
        }
        return reservedStocks
    }

    private fun reserveCouponIfPresent(
        couponId: Long?,
        userId: Long,
        reservedStocks: List<Pair<Long, Int>>,
    ): Boolean {
        return couponId?.let { id ->
            if (!couponReservationRepository.reserve(id, userId)) {
                reservedStocks.forEach { (productId, qty) -> stockReservationRepository.restore(productId, qty) }
                throw CoreException(ErrorType.CONFLICT, "이미 사용된 쿠폰입니다.")
            }
            true
        } ?: false
    }

    private fun validateCouponIfPresent(couponId: Long?, userId: Long): CouponApplyInfo? {
        return couponId?.let { id ->
            val issuedCoupon = couponService.findIssuedCouponByCouponIdAndUserId(id, userId)
            val coupon = couponService.findCouponById(id)
            issuedCoupon.validateUsable(coupon.expiresAt)
            CouponApplyInfo(id, coupon.discount)
        }
    }

    private fun buildOrderItemCommands(
        items: List<OrderPlaceCommand>,
        productMap: Map<Long, Product>,
    ): List<OrderItemCommand> {
        val brandMap = brandService.getBrandsByIds(
            productMap.values.map { it.brandId }.distinct(),
        ).associateBy { it.id }

        return items.map { item ->
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
    }

    private fun publishOrderCompletedEvent(
        orderId: Long,
        userId: Long,
        items: List<OrderPlaceCommand>,
        productMap: Map<Long, Product>,
        couponId: Long?,
        totalAmount: Long,
        paymentAmount: Long,
    ) {
        outboxPublisher.publish(
            aggregateType = AggregateTypes.ORDER,
            aggregateId = orderId.toString(),
            eventType = EventTypes.ORDER_COMPLETED,
            version = System.currentTimeMillis(),
            payload = OrderCompletedPayload(
                orderId = orderId,
                userId = userId,
                items = items.map {
                    OrderItemPayload(it.productId, it.quantity.value, productMap[it.productId]?.name ?: "")
                },
                couponId = couponId,
                totalAmount = totalAmount,
                paymentAmount = paymentAmount,
            ),
        )
    }

    private fun restoreReservations(
        reservedStocks: List<Pair<Long, Int>>,
        couponId: Long?,
        userId: Long,
        couponReserved: Boolean,
    ) {
        reservedStocks.forEach { (productId, qty) -> stockReservationRepository.restore(productId, qty) }
        if (couponReserved) couponReservationRepository.restore(couponId!!, userId)
    }

    private data class CouponApplyInfo(
        val couponId: Long,
        val discount: Discount,
    )
}
