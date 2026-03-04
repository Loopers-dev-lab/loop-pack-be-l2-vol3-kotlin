package com.loopers.application.order

import com.loopers.application.brand.BrandService
import com.loopers.application.coupon.CouponService
import com.loopers.application.product.ProductService
import com.loopers.application.product.ReservedProduct
import com.loopers.domain.order.OrderItemCommand
import com.loopers.domain.product.Product
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
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
    fun createOrder(userId: Long, criteria: List<OrderItemCriteria>, couponId: Long? = null): OrderResultInfo {
        val productIds = criteria.map { it.productId }
        val products = productService.getProductsWithLock(productIds)

        if (couponId != null) {
            return createOrderWithCoupon(userId, criteria, couponId, products)
        }
        return createOrderWithoutCoupon(userId, criteria, products)
    }

    private fun createOrderWithoutCoupon(
        userId: Long,
        criteria: List<OrderItemCriteria>,
        products: List<Product>,
    ): OrderResultInfo {
        val reservation = productService.reserveStock(products, criteria)

        if (reservation.reservedProducts.isEmpty()) {
            val reasons = reservation.failedReservations.joinToString(", ") { it.reason }
            throw CoreException(ErrorType.BAD_REQUEST, "주문 가능한 상품이 없습니다. ($reasons)")
        }

        val orderItemCommands = buildOrderItemCommands(reservation.reservedProducts)
        val order = orderService.createOrder(userId, orderItemCommands)

        val excludedItems = reservation.failedReservations.map {
            ExcludedItemInfo(it.productId, it.reason)
        }
        return OrderResultInfo.of(order, excludedItems)
    }

    private fun createOrderWithCoupon(
        userId: Long,
        criteria: List<OrderItemCriteria>,
        couponId: Long,
        products: List<Product>,
    ): OrderResultInfo {
        // 쿠폰 사용 시 부분 주문 불가 — 전체 재고 확인
        val reservation = productService.reserveStock(products, criteria)

        if (reservation.failedReservations.isNotEmpty()) {
            val reasons = reservation.failedReservations.joinToString(", ") { it.reason }
            throw CoreException(ErrorType.BAD_REQUEST, "쿠폰 사용 주문은 부분 주문이 불가합니다. ($reasons)")
        }

        // 비관적 락으로 발급 쿠폰 조회
        val issuedCoupon = couponService.getIssuedCouponWithLock(couponId)
        issuedCoupon.validateOwner(userId)
        issuedCoupon.validateUsable()

        // 쿠폰 정보 조회 및 할인 계산
        val coupon = couponService.getCoupon(issuedCoupon.couponId)

        val orderItemCommands = buildOrderItemCommands(reservation.reservedProducts)
        val order = orderService.createOrder(userId, orderItemCommands, couponId)

        val originalAmount = order.originalAmount
        coupon.validateMinOrderAmount(originalAmount)
        val discountAmount = coupon.calculateDiscount(originalAmount)

        // 쿠폰 사용 처리 + 할인 적용
        issuedCoupon.use()
        order.applyDiscount(discountAmount)

        return OrderResultInfo.of(order, emptyList())
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
