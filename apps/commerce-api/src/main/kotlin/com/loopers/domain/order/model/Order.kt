package com.loopers.domain.order.model

import com.loopers.domain.common.vo.CouponId
import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.annotation.AggregateRootOnly
import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.order.OrderProductData
import com.loopers.domain.common.vo.Quantity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.math.BigDecimal
import java.time.ZonedDateTime

class Order private constructor(
    val id: OrderId = OrderId(0),
    val refUserId: UserId,
    status: OrderStatus,
    val originalPrice: Money,
    discountAmount: Money,
    totalPrice: Money,
    refCouponId: CouponId? = null,
    val items: List<OrderItem> = emptyList(),
    val deletedAt: ZonedDateTime? = null,
) {
    var status: OrderStatus = status
        private set

    var discountAmount: Money = discountAmount
        private set

    var totalPrice: Money = totalPrice
        private set

    var refCouponId: CouponId? = refCouponId
        private set

    fun isDeleted(): Boolean = deletedAt != null

    enum class OrderStatus {
        CREATED,
        PENDING_PAYMENT,
        PAID,
        CANCELLED,
        FAILED,
    }

    fun markPendingPayment() {
        if (status != OrderStatus.CREATED && status != OrderStatus.FAILED) {
            throw CoreException(ErrorType.BAD_REQUEST, "생성 또는 실패 상태의 주문만 결제 대기 상태로 전환할 수 있습니다.")
        }
        status = OrderStatus.PENDING_PAYMENT
    }

    fun markPaid() {
        if (status != OrderStatus.PENDING_PAYMENT) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 대기 상태인 주문만 결제 완료 상태로 전환할 수 있습니다.")
        }
        status = OrderStatus.PAID
    }

    fun markFailed() {
        if (status != OrderStatus.PENDING_PAYMENT) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 대기 상태인 주문만 결제 실패 상태로 전환할 수 있습니다.")
        }
        status = OrderStatus.FAILED
    }

    fun applyDiscount(discountAmount: Money, refCouponId: CouponId) {
        if (status != OrderStatus.CREATED) {
            throw CoreException(ErrorType.BAD_REQUEST, "생성된 주문에만 할인을 적용할 수 있습니다.")
        }
        if (this.refCouponId != null) {
            throw CoreException(ErrorType.BAD_REQUEST, "이미 할인이 적용된 주문입니다.")
        }
        if (discountAmount.value > originalPrice.value) {
            throw CoreException(ErrorType.BAD_REQUEST, "할인 금액은 원래 가격을 초과할 수 없습니다.")
        }
        this.discountAmount = discountAmount
        this.totalPrice = originalPrice - discountAmount
        this.refCouponId = refCouponId
    }

    @OptIn(AggregateRootOnly::class)
    fun cancelItem(item: OrderItem) {
        item.cancel()
        val activeItemsTotal = items
            .filter { it.status == OrderItem.ItemStatus.ACTIVE }
            .fold(Money(BigDecimal.ZERO)) { acc, it -> acc + (it.productPrice * it.quantity.value) }
        val applicableDiscount = if (discountAmount.value <= activeItemsTotal.value) discountAmount else activeItemsTotal
        discountAmount = applicableDiscount
        totalPrice = activeItemsTotal - applicableDiscount
    }

    @OptIn(AggregateRootOnly::class)
    fun assignOrderIdToItems(orderId: OrderId) {
        items.forEach { it.assignToOrder(orderId) }
    }

    companion object {
        fun create(
            userId: UserId,
            items: List<Pair<OrderProductData, Quantity>>,
        ): Order {
            if (items.isEmpty()) {
                throw CoreException(ErrorType.BAD_REQUEST, "주문은 최소 하나 이상의 항목을 포함해야 합니다.")
            }
            val orderItems = items.map { (info, quantity) ->
                OrderItem.create(info, quantity)
            }
            val originalPrice = orderItems.fold(Money(BigDecimal.ZERO)) { acc, item ->
                acc + (item.productPrice * item.quantity.value)
            }
            return Order(
                refUserId = userId,
                status = OrderStatus.CREATED,
                originalPrice = originalPrice,
                discountAmount = Money(BigDecimal.ZERO),
                totalPrice = originalPrice,
                items = orderItems,
            )
        }

        fun fromPersistence(
            id: OrderId,
            refUserId: UserId,
            status: OrderStatus,
            originalPrice: Money,
            discountAmount: Money,
            totalPrice: Money,
            refCouponId: CouponId?,
            deletedAt: ZonedDateTime?,
            items: List<OrderItem> = emptyList(),
        ): Order {
            return Order(
                id = id,
                refUserId = refUserId,
                status = status,
                originalPrice = originalPrice,
                discountAmount = discountAmount,
                totalPrice = totalPrice,
                refCouponId = refCouponId,
                items = items,
                deletedAt = deletedAt,
            )
        }
    }
}
