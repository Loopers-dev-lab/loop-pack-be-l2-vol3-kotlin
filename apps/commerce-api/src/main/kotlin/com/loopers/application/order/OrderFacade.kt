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

        // 1. 재고 예약 (실패 시 예외)
        val reservedProducts = productService.reserveStock(products, criteria)

        // 2. 주문 아이템 조립
        val orderItemCommands = buildOrderItemCommands(reservedProducts)

        // 3. 쿠폰 적용 (optional)
        if (couponId != null) {
            val issuedCoupon = couponService.getIssuedCouponWithLock(couponId)
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
