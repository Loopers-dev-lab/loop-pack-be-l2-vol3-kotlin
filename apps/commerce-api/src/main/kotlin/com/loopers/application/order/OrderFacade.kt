package com.loopers.application.order

import com.loopers.domain.brand.BrandService
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.Discount
import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.order.OrderItemCommand
import com.loopers.domain.order.OrderService
import com.loopers.domain.product.ProductService
import com.loopers.domain.product.StockDeductionRequest
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class OrderFacade(
    private val orderService: OrderService,
    private val productService: ProductService,
    private val brandService: BrandService,
    private val couponService: CouponService,
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
    fun placeOrder(userId: Long, items: List<OrderPlaceCommand>, couponId: Long? = null) {
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

        val order = orderService.createOrder(userId, orderItemCommands)

        // 쿠폰 할인 적용
        couponInfo?.let {
            val discountAmount = it.discount.calculateDiscountAmount(order.totalAmount)
            order.applyCouponDiscount(it.couponId, discountAmount)
            it.issuedCoupon.use()
        }
    }

    private data class CouponApplyInfo(
        val couponId: Long,
        val discount: Discount,
        val issuedCoupon: IssuedCoupon,
    )
}
