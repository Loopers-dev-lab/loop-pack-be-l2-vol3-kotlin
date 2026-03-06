package com.loopers.application.order

import com.loopers.domain.brand.BrandService
import com.loopers.domain.coupon.CouponIssueService
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.order.OrderModel
import com.loopers.domain.order.OrderService
import com.loopers.domain.product.ProductService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderFacade(
    private val orderService: OrderService,
    private val productService: ProductService,
    private val brandService: BrandService,
    private val couponIssueService: CouponIssueService,
    private val couponService: CouponService,
) {
    @Transactional
    fun createOrder(userId: Long, items: List<OrderItemRequest>, couponIssueId: Long? = null): OrderInfo {
        validateNoDuplicateProducts(items)

        val productIds = items.map { it.productId }
        val productMap = productService.findAllByIdsForUpdate(productIds).associateBy { it.id }

        items.forEach { item ->
            productMap[item.productId]
                ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다: ${item.productId}")
        }

        // 쿠폰 검증 (재고 변경 전에 실패 가능성 높은 검증을 먼저 수행)
        if (couponIssueId != null) {
            validateCoupon(userId, couponIssueId)
        }

        val brandIds = productMap.values.map { it.brandId }.distinct()
        val brandMap = brandService.findAllByIds(brandIds).associateBy { it.id }

        val orderItems = items.map { item ->
            val product = productMap[item.productId]
                ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다: ${item.productId}")
            product to item.quantity
        }

        val order = orderService.createOrder(
            userId = userId,
            orderItems = orderItems,
            brandNameResolver = { brandId ->
                brandMap[brandId]?.name
                    ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다: $brandId")
            },
        )

        // 쿠폰 적용
        if (couponIssueId != null) {
            applyCoupon(userId, couponIssueId, order)
        }

        return OrderInfo.from(order)
    }

    private fun validateCoupon(userId: Long, couponIssueId: Long) {
        val couponIssue = couponIssueService.findById(couponIssueId)
        if (couponIssue.userId != userId) {
            throw CoreException(ErrorType.BAD_REQUEST, "본인 소유의 쿠폰만 사용할 수 있습니다.")
        }
        if (!couponIssue.isUsable()) {
            throw CoreException(ErrorType.BAD_REQUEST, "사용할 수 없는 쿠폰입니다.")
        }
        val coupon = couponService.findById(couponIssue.couponId)
        if (coupon.isExpired()) {
            throw CoreException(ErrorType.BAD_REQUEST, "만료된 쿠폰은 사용할 수 없습니다.")
        }
    }

    private fun applyCoupon(userId: Long, couponIssueId: Long, order: OrderModel) {
        val couponIssue = couponIssueService.findByIdForUpdate(couponIssueId)
        val coupon = couponService.findById(couponIssue.couponId)

        couponIssue.use()
        val discountAmount = coupon.calculateDiscount(order.originalTotalAmount)
        order.applyCouponDiscount(couponIssueId, discountAmount)
    }

    private fun validateNoDuplicateProducts(items: List<OrderItemRequest>) {
        val productIds = items.map { it.productId }
        if (productIds.size != productIds.distinct().size) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문에 중복된 상품이 포함되어 있습니다.")
        }
    }
}

data class OrderItemRequest(
    val productId: Long,
    val quantity: Int,
)
