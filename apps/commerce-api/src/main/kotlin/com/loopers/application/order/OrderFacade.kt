package com.loopers.application.order

import com.loopers.domain.Money
import com.loopers.domain.brand.BrandService
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.event.ActionType
import com.loopers.domain.event.UserActionEvent
import com.loopers.domain.order.CreateOrderCommand
import com.loopers.domain.order.OrderInfo
import com.loopers.domain.order.OrderService
import com.loopers.domain.order.event.OrderEvent
import com.loopers.domain.product.ProductService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.domain.event.DomainEventPublisher
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
    private val domainEventPublisher: DomainEventPublisher,
) {

    /**
     * 주문 생성.
     *
     * @Transactional 선택 이유:
     * - 재고 차감 + 쿠폰 검증 + 주문 생성이 하나의 원자적 작업이어야 함
     * - 쿠폰 USED 마킹은 핵심 트랜잭션에서 제외 → AFTER_COMMIT 이벤트로 분리
     *   (할인 계산은 동기로 처리, 실제 상태 변경만 비동기)
     *
     * 이벤트 발행:
     * - OrderEvent.Created를 트랜잭션 내에서 발행
     * - @TransactionalEventListener(AFTER_COMMIT) 리스너가 커밋 후 후속 처리
     *   (쿠폰 USED 마킹, 로깅, 알림)
     */
    @Transactional
    fun createOrder(userId: Long, criteria: CreateOrderCriteria): OrderResult {
        val productIds = criteria.items.map { it.productId }
        val products = productService.findByIds(productIds)
        if (products.size != productIds.toSet().size) {
            throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품이 포함되어 있습니다.")
        }

        val quantities = criteria.items.associate { it.productId to it.quantity }

        // ── 쿠폰 적용 (검증 + 할인 계산만, USED 마킹은 AFTER_COMMIT 이벤트에서 처리) ──
        var couponIssueId: Long? = null
        var couponDiscountAmount = Money.ZERO

        if (criteria.couponIssueId != null) {
            val rawTotal = products.fold(Money.ZERO) { acc, product ->
                acc + product.price * quantities[product.id]!!
            }
            val usage = couponService.validateAndCalculateDiscount(criteria.couponIssueId, userId, rawTotal)
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

        // ── 이벤트 발행 (커밋 후 후속 처리 트리거) ──
        domainEventPublisher.publish(
            OrderEvent.Created(
                aggregateId = order.id,
                userId = userId,
                orderNumber = order.orderNumber,
                totalAmount = order.totalAmount,
                couponIssueId = couponIssueId,
                couponDiscountAmount = couponDiscountAmount,
                orderItems = order.orderItems.map { item ->
                    OrderEvent.OrderItemSnapshot(
                        productId = item.productId,
                        productName = item.productSnapshot.productName,
                        quantity = item.quantity,
                        unitPrice = item.priceSnapshot.originalPrice,
                    )
                },
            ),
        )

        // ── 유저 행동 로깅 ──
        domainEventPublisher.publish(
            UserActionEvent(
                aggregateId = order.id,
                userId = userId,
                actionType = ActionType.ORDER,
                targetId = order.id,
                metadata = mapOf("orderNumber" to order.orderNumber),
            ),
        )

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
