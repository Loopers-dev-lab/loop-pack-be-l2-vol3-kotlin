package com.loopers.application.handler.order

import com.loopers.application.handler.coupon.UseCouponCommandHandler
import com.loopers.application.handler.product.DeductStockCommandHandler
import com.loopers.application.order.OrderService
import com.loopers.application.product.ProductService
import com.loopers.application.brand.BrandService
import com.loopers.application.coupon.CouponService
import com.loopers.application.order.OrderCommand
import com.loopers.domain.common.command.DeductStockCommand
import com.loopers.domain.common.command.UseCouponCommand
import com.loopers.domain.common.event.OrderRequestedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class OrderRequestedEventHandler(
    private val deductStockCommandHandler: DeductStockCommandHandler,
    private val useCouponCommandHandler: UseCouponCommandHandler,
    private val orderService: OrderService,
    private val productService: ProductService,
    private val brandService: BrandService,
    private val couponService: CouponService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: OrderRequestedEvent) {
        try {
            // 1. 재고 차감 (각 상품별, productId 오름차순 — 데드락 방지)
            event.items.sortedBy { it.productId }.forEach { item ->
                deductStockCommandHandler.handle(
                    DeductStockCommand(productId = item.productId, quantity = item.quantity),
                )
            }

            // 2. 쿠폰 사용
            if (event.couponId != null) {
                useCouponCommandHandler.handle(
                    UseCouponCommand(issuedCouponId = event.couponId, memberId = event.memberId),
                )
            }

            // 3. 상품 정보 조회 (스냅샷)
            val productIds = event.items.map { it.productId }
            val products = productService.getProductsByIds(productIds).associateBy { it.id }

            // 4. 브랜드명 조회
            val brandIds = products.values.map { it.brandId }.distinct()
            val brandNames = brandIds.associateWith { brandId ->
                runCatching { brandService.getBrand(brandId).name }.getOrDefault("")
            }

            // 5. 쿠폰 할인 계산
            var discountAmount = 0L
            if (event.couponId != null) {
                val issuedCoupon = couponService.getIssuedCouponById(event.couponId)
                val template = couponService.getTemplate(issuedCoupon.couponTemplateId)
                val orderAmount = event.items.sumOf { item ->
                    val product = products[item.productId]!!
                    product.price * item.quantity
                }
                discountAmount = template.calculateDiscount(orderAmount)
            }

            // 6. 주문 생성
            val orderItems = event.items.map { item ->
                val product = products[item.productId]!!
                OrderCommand.CreateOrderItem(productId = item.productId, quantity = item.quantity)
            }

            orderService.createOrder(
                memberId = event.memberId,
                products = products,
                brandNames = brandNames,
                items = orderItems,
                couponId = event.couponId,
                discountAmount = discountAmount,
            )
        } catch (e: Exception) {
            log.error("주문 요청 처리 실패: memberId={}, error={}", event.memberId, e.message, e)
            // 보상은 Polling 배치에 전임
        }
    }
}
