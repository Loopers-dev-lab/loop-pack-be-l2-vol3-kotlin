package com.loopers.application.handler.order

import com.loopers.application.brand.BrandService
import com.loopers.application.coupon.CouponService
import com.loopers.application.order.OrderCommand
import com.loopers.application.order.OrderService
import com.loopers.application.product.ProductService
import com.loopers.domain.common.command.CreateOrderCommand
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class CreateOrderCommandHandler(
    private val orderService: OrderService,
    private val productService: ProductService,
    private val brandService: BrandService,
    private val couponService: CouponService,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(command: CreateOrderCommand) {
        // 1. 상품 정보 조회 (스냅샷)
        val products = productService.getProductsByIds(
            command.items.map { it.productId },
        ).associateBy { it.id }

        // 2. 브랜드명 조회
        val brandNames = products.values.map { it.brandId }.distinct().associateWith { brandId ->
            runCatching { brandService.getBrand(brandId).name }.getOrDefault("")
        }

        // 3. 쿠폰 할인 계산
        var discountAmount = 0L
        if (command.couponId != null) {
            val issuedCoupon = couponService.getIssuedCouponById(command.couponId)
            val template = couponService.getTemplate(issuedCoupon.couponTemplateId)
            val orderAmount = command.items.sumOf { item ->
                val product = products[item.productId]!!
                product.price * item.quantity
            }
            discountAmount = template.calculateDiscount(orderAmount)
        }

        // 4. 주문 생성
        val orderItems = command.items.map { item ->
            OrderCommand.CreateOrderItem(
                productId = item.productId,
                quantity = item.quantity,
            )
        }

        orderService.createOrder(
            memberId = command.memberId,
            products = products,
            brandNames = brandNames,
            items = orderItems,
            couponId = command.couponId,
            discountAmount = discountAmount,
        )
    }
}
