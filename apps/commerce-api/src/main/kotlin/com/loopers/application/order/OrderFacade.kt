package com.loopers.application.order

import com.loopers.domain.Money
import com.loopers.domain.brand.BrandService
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.order.CreateOrderCommand
import com.loopers.domain.order.OrderInfo
import com.loopers.domain.order.OrderService
import com.loopers.domain.product.ProductService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderFacade(
    private val productService: ProductService,
    private val brandService: BrandService,
    private val orderService: OrderService,
    private val couponService: CouponService,
) {

    /**
     * 주문 생성.
     *
     * @Transactional 선택 이유:
     * - 재고 차감 + 쿠폰 사용 + 주문 생성이 하나의 원자적 작업이어야 함
     * - 이 메서드가 여러 도메인(Product, Coupon, Order)을 조율하므로
     *   트랜잭션 경계를 Facade에서 잡는 것이 적절
     * - 쿠폰 검증/사용 비즈니스 로직은 CouponService.useCouponForOrder()에 위임
     */
    @Transactional
    fun createOrder(userId: Long, criteria: CreateOrderCriteria): OrderResult {
        val productIds = criteria.items.map { it.productId }
        val products = productService.findByIds(productIds)
        if (products.size != productIds.toSet().size) {
            throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품이 포함되어 있습니다.")
        }

        val quantities = criteria.items.associate { it.productId to it.quantity }

        // ── 쿠폰 적용 (검증 + 할인 계산 + 사용 처리는 CouponService에 위임) ──
        var couponIssueId: Long? = null
        var couponDiscountAmount = Money.ZERO

        if (criteria.couponIssueId != null) {
            val rawTotal = products.fold(Money.ZERO) { acc, product ->
                acc + product.price * quantities[product.id]!!
            }
            val usage = couponService.useCouponForOrder(criteria.couponIssueId, userId, rawTotal)
            couponIssueId = usage.couponIssueId
            couponDiscountAmount = usage.discountAmount
        }

        // ── 재고 차감 (ID 오름차순 정렬 → 데드락 방지) ──
        // 다중 상품 주문 시 모든 트랜잭션이 동일한 순서로 row lock을 획득하도록 보장
        products.sortedBy { it.id }.forEach { product ->
            val qty = quantities[product.id]!!
            productService.decreaseStock(product.id, qty)
        }

        // ── 주문 생성 ──
        val brandIds = products.map { it.brandId }.distinct()
        val brands = brandService.findByIds(brandIds).associateBy { it.id }

        val command = CreateOrderCommand(
            userId = userId,
            products = products,
            quantities = quantities,
            brands = brands,
            couponIssueId = couponIssueId,
            couponDiscountAmount = couponDiscountAmount,
        )
        val order = orderService.createOrder(command)

        return OrderResult.from(OrderInfo.from(order))
    }

    fun getOrders(userId: Long, criteria: GetOrdersCriteria): List<OrderResult> {
        val orders = orderService.findByUserIdAndCreatedAtBetween(userId, criteria.startAt, criteria.endAt)
        return orders.map { OrderResult.from(OrderInfo.from(it)) }
    }

    fun getOrder(userId: Long, orderId: Long): OrderResult {
        val order = orderService.findById(orderId)
        if (order.userId != userId) {
            throw CoreException(ErrorType.FORBIDDEN, "접근 권한이 없습니다.")
        }
        return OrderResult.from(OrderInfo.from(order))
    }

    fun getOrdersForAdmin(pageable: Pageable): Page<OrderResult> {
        return orderService.findAll(pageable)
            .map { OrderResult.from(OrderInfo.from(it)) }
    }

    fun getOrderForAdmin(orderId: Long): OrderResult {
        val order = orderService.findById(orderId)
        return OrderResult.from(OrderInfo.from(order))
    }
}
