package com.loopers.application.order

import com.loopers.application.brand.BrandService
import com.loopers.application.coupon.CouponService
import com.loopers.application.product.ProductService
import com.loopers.domain.common.event.OrderRequestedEvent
import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderPreparationService(
    private val productService: ProductService,
    private val brandService: BrandService,
    private val couponService: CouponService,
) {
    @Transactional(readOnly = true)
    fun prepare(memberId: Long, command: OrderCommand.Create): OrderRequestedEvent {
        if (command.items.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 항목은 최소 1개 이상이어야 합니다.")
        }

        val products = productService.getProductsByIds(
            command.items.map { it.productId },
        ).associateBy { it.id }

        val brandNames = products.values.map { it.brandId }.distinct().associateWith { brandId ->
            brandService.getBrand(brandId).name
        }

        val items = command.items.map { item ->
            val product = products[item.productId]
                ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다. productId=${item.productId}")
            val brandName = brandNames[product.brandId] ?: ""

            OrderRequestedEvent.OrderRequestedItem(
                productId = product.id,
                quantity = item.quantity,
                productName = product.name,
                productPrice = product.price,
                brandId = product.brandId,
                brandName = brandName,
            )
        }

        val orderAmount = items.sumOf { it.productPrice * it.quantity }

        val discountAmount = if (command.couponId != null) {
            val issuedCoupon = couponService.getIssuedCouponById(command.couponId)
            val template = couponService.getTemplate(issuedCoupon.couponTemplateId)
            template.calculateDiscount(orderAmount)
        } else {
            0L
        }

        val finalAmount = orderAmount - discountAmount

        return OrderRequestedEvent(
            memberId = memberId,
            items = items,
            couponId = command.couponId,
            discountAmount = discountAmount,
            orderAmount = orderAmount,
            finalAmount = finalAmount,
        )
    }
}
