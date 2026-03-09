package com.loopers.application.order

import com.loopers.application.brand.BrandService
import com.loopers.application.coupon.CouponService
import com.loopers.application.product.ProductService
import com.loopers.application.product.ReservedProduct
import com.loopers.domain.order.OrderItemCommand
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderFacade(
    private val orderService: OrderService,
    private val productService: ProductService,
    private val brandService: BrandService,
    private val couponService: CouponService,
) {

    @Transactional
    fun createOrder(userId: Long, criteria: List<OrderItemCriteria>, couponId: Long? = null): OrderInfo {
        val productIds = criteria.map { it.productId }
        val products = productService.getProductsWithLock(productIds)

        val reservedProducts = productService.reserveStock(products, criteria)

        val orderItemCommands = buildOrderItemCommands(reservedProducts)

        if (couponId != null) {
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

            return OrderInfo.from(order)
        }

        // 4. 쿠폰 없는 주문
        val order = orderService.createOrder(userId, orderItemCommands)
        return OrderInfo.from(order)
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
