package com.loopers.application.order

import com.loopers.application.brand.BrandService
import com.loopers.application.coupon.CouponService
import com.loopers.application.product.ProductService
import com.loopers.application.useraction.LogUserAction
import com.loopers.domain.common.event.OrderRequestedEvent
import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import com.loopers.domain.useraction.UserActionTargetType
import com.loopers.domain.useraction.UserActionType
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class OrderFacade(
    private val orderService: OrderService,
    private val productService: ProductService,
    private val brandService: BrandService,
    private val couponService: CouponService,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @LogUserAction(action = UserActionType.ORDER, targetType = UserActionTargetType.PRODUCT)
    @Transactional
    fun createOrder(memberId: Long, command: OrderCommand.Create) {
        if (command.items.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 항목은 최소 1개 이상이어야 합니다.")
        }
        if (command.items.any { it.quantity <= 0 }) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 수량은 1 이상이어야 합니다.")
        }

        val products = productService.getProductsByIds(
            command.items.map { it.productId },
        ).associateBy { it.id }

        val brandNames = products.values.map { it.brandId }.distinct().associateWith { brandId ->
            brandService.getBrand(brandId).name
        }

        val items = command.items.map { item ->
            val product = products[item.productId]
                ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다. productId=${item.productId}")
            val brandName = brandNames[product.brandId] ?: ""

            OrderRequestedEvent.OrderRequestedItem(
                productId = product.id,
                quantity = item.quantity,
                productName = product.name,
                productPrice = product.price,
                brandId = product.brandId,
                brandName = brandName,
            )
        }

        val orderAmount = items.sumOf { it.productPrice * it.quantity }

        val discountAmount = if (command.couponId != null) {
            val issuedCoupon = couponService.getIssuedCouponById(command.couponId)
            val template = couponService.getTemplate(issuedCoupon.couponTemplateId)
            template.calculateDiscount(orderAmount)
        } else {
            0L
        }

        val finalAmount = orderAmount - discountAmount

        val event = OrderRequestedEvent(
            memberId = memberId,
            items = items,
            couponId = command.couponId,
            discountAmount = discountAmount,
            orderAmount = orderAmount,
            finalAmount = finalAmount,
        )
        eventPublisher.publishEvent(event)
    }

    @Transactional(readOnly = true)
    fun getOrders(memberId: Long, startAt: ZonedDateTime, endAt: ZonedDateTime): List<OrderInfo> {
        return orderService.getOrdersByMember(memberId, startAt, endAt)
            .map { OrderInfo.from(it) }
    }

    @Transactional(readOnly = true)
    fun getOrder(memberId: Long, orderId: Long): OrderInfo {
        return orderService.getOrder(orderId, memberId)
            .let { OrderInfo.from(it) }
    }
}
