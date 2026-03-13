package com.loopers.application.order

import com.loopers.application.brand.BrandService
import com.loopers.application.coupon.CouponService
import com.loopers.application.product.ProductService
import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.CouponTemplateModel
import com.loopers.domain.coupon.IssuedCouponModel
import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class OrderFacade(
    private val orderService: OrderService,
    private val productService: ProductService,
    private val brandService: BrandService,
    private val couponService: CouponService,
) {
    @Transactional
    fun createOrder(memberId: Long, command: OrderCommand.Create): OrderInfo {
        // 1. Coupon validation (fail-fast BEFORE any lock)
        var discountAmount = 0L
        var couponId: Long? = null
        var issuedCoupon: IssuedCouponModel? = null
        var template: CouponTemplateModel? = null

        if (command.couponId != null) {
            issuedCoupon = couponService.getIssuedCouponById(command.couponId)
            if (issuedCoupon.memberId != memberId) {
                throw CoreException(ErrorType.FORBIDDEN, "본인의 쿠폰만 사용할 수 있습니다.")
            }
            if (issuedCoupon.status != CouponStatus.AVAILABLE) {
                throw CoreException(ErrorType.BAD_REQUEST, "사용 불가능한 쿠폰입니다.")
            }
            if (issuedCoupon.isExpired()) {
                throw CoreException(ErrorType.BAD_REQUEST, "만료된 쿠폰입니다.")
            }
            template = couponService.getTemplate(issuedCoupon.couponTemplateId)
        }

        // 2. Coupon use (@Version + flush — 재고 락 전에 쿠폰 충돌 조기 감지)
        if (issuedCoupon != null) {
            try {
                couponService.saveIssuedCoupon(issuedCoupon.use())
                couponId = issuedCoupon.id
            } catch (e: OptimisticLockingFailureException) {
                throw CoreException(ErrorType.CONFLICT, "쿠폰이 이미 사용되었습니다.")
            }
        }

        // 3. Stock deduction (pessimistic lock, sorted by productId for deadlock prevention)
        command.items.sortedBy { it.productId }.forEach { item ->
            productService.deductStock(item.productId, item.quantity)
        }

        // 4. Product lookup
        val productIds = command.items.map { it.productId }
        val products = productService.getProductsByIds(productIds)
        val productMap = products.associateBy { it.id }

        // 5. Coupon discount calculation
        if (template != null) {
            val originalAmount = command.items.sumOf { item ->
                val product = productMap[item.productId]!!
                product.price * item.quantity
            }
            template.validateMinOrderAmount(originalAmount)
            discountAmount = template.calculateDiscount(originalAmount)
        }

        // 6. Brand names
        val brandIds = products.map { it.brandId }.distinct()
        val brandNames = brandIds.associateWith { brandId ->
            runCatching { brandService.getBrand(brandId) }.getOrNull()?.name ?: ""
        }

        // 7. Create order
        return orderService.createOrder(memberId, productMap, brandNames, command.items, couponId, discountAmount)
            .let { OrderInfo.from(it) }
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
