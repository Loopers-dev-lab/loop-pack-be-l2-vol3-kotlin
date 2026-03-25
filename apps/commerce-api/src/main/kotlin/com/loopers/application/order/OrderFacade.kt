package com.loopers.application.order

import com.loopers.application.brand.BrandService
import com.loopers.application.coupon.CouponService
import com.loopers.application.event.OrderCreatedEvent
import com.loopers.application.product.ProductService
import com.loopers.application.product.ReservedProduct
import com.loopers.domain.order.OrderItemCommand
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderFacade(
    private val orderService: OrderService,
    private val productService: ProductService,
    private val brandService: BrandService,
    private val couponService: CouponService,
    private val eventPublisher: ApplicationEventPublisher,
) {

    @Transactional
    fun createOrder(userId: Long, criteria: List<OrderItemCriteria>, couponId: Long? = null): OrderInfo {
        val productIds = criteria.map { it.productId }
        val products = productService.getProductsWithLock(productIds)

        val reservedProducts = productService.reserveStock(products, criteria)

        val orderItemCommands = buildOrderItemCommands(reservedProducts)

        val orderInfo = if (couponId != null) {
            val issuedCoupon = couponService.getIssuedCoupon(couponId)
            issuedCoupon.validateOwner(userId)
            issuedCoupon.validateUsable()

            val coupon = couponService.getCoupon(issuedCoupon.couponId)
            val order = orderService.createOrder(userId, orderItemCommands, couponId)

            val originalAmount = order.originalAmount
            coupon.validateMinOrderAmount(originalAmount)
            val discountAmount = coupon.calculateDiscount(originalAmount)

            issuedCoupon.use()
            order.applyDiscount(discountAmount)

            OrderInfo.from(order)
        } else {
            val order = orderService.createOrder(userId, orderItemCommands)
            OrderInfo.from(order)
        }

        eventPublisher.publishEvent(
            OrderCreatedEvent(
                orderId = orderInfo.id,
                userId = userId,
                productIds = productIds,
                totalAmount = orderInfo.totalAmount,
                couponId = couponId,
            ),
        )

        return orderInfo
    }

    private fun buildOrderItemCommands(
        reservedProducts: List<ReservedProduct>,
    ): List<OrderItemCommand> {
        val brandIds = reservedProducts.map { it.brandId }.distinct()
        val brandMap = brandService.getBrandsIncludingDeleted(brandIds).associateBy { it.id }

        return reservedProducts.map { reserved ->
            OrderItemCommand(
                productId = reserved.productId,
                productName = reserved.productName,
                brandName = brandMap[reserved.brandId]?.name ?: "-",
                quantity = reserved.quantity,
                unitPrice = reserved.unitPrice,
            )
        }
    }
}
