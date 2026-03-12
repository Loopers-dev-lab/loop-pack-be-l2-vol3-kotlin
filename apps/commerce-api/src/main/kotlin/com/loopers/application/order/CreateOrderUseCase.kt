package com.loopers.application.order

import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.UserCoupon
import com.loopers.domain.coupon.UserCouponRepository
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import com.loopers.domain.order.OrderItemRepository
import com.loopers.domain.order.OrderItemSnapshot
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.order.OrderValidator
import com.loopers.domain.product.Money
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStockRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.CouponErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateOrderUseCase(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val productRepository: ProductRepository,
    private val productStockRepository: ProductStockRepository,
    private val brandRepository: BrandRepository,
    private val orderValidator: OrderValidator,
    private val userCouponRepository: UserCouponRepository,
    private val couponRepository: CouponRepository,
) {

    @Transactional
    fun execute(command: OrderCommand.Create): OrderInfo {
        val orderLines = command.toOrderLines()

        // 1. 쿠폰 사전 검증 (락 전, fast-fail)
        val (userCoupon, coupon) = validateCoupon(command.couponId, command.userId)

        // 2. 상품 정보 조회 (락 없이)
        val productIds = orderLines.map { it.productId }.distinct()
        val products = productRepository.findAllByIds(productIds)
        val productMap = products.associateBy { it.id }

        // 3. 데드락 방지를 위해 productId 오름차순으로 재고 비관적 락 획득
        val sortedProductIds = productIds.sorted()
        val productStockMap = sortedProductIds.mapNotNull { productId ->
            productStockRepository.findByProductIdForUpdate(productId)
        }.associateBy { it.productId }

        orderValidator.validate(orderLines, productMap, productStockMap)

        val brandIds = products.map { it.brandId }.distinct()
        val brandMap = brandRepository.findAllByIds(brandIds).associateBy { it.id }

        val snapshots = orderLines.map { line ->
            val product = productMap.getValue(line.productId)
            val brand = brandMap[product.brandId]
            OrderItemSnapshot(
                productId = product.id,
                productName = product.name,
                productPrice = product.price,
                brandName = brand?.name ?: "",
                imageUrl = product.imageUrl,
                quantity = line.quantity,
            )
        }

        // 4. 할인 계산
        val originalAmount = Money(snapshots.sumOf { it.productPrice.amount * it.quantity.value })
        val discountAmount = calculateDiscount(coupon, originalAmount)

        // 5. 주문 생성 + 재고 차감
        val order = Order.create(
            userId = command.userId,
            items = snapshots,
            discountAmount = discountAmount,
            userCouponId = userCoupon?.id,
        )
        val savedOrder = orderRepository.save(order)

        val orderItems = snapshots.map { snapshot ->
            OrderItem.create(orderId = savedOrder.id, snapshot = snapshot)
        }
        orderItemRepository.saveAll(orderItems)

        orderLines.forEach { line ->
            val productStock = productStockMap.getValue(line.productId)
            productStock.decreaseStock(line.quantity.value)
        }

        // 6. 쿠폰 Conditional Update
        userCoupon?.let {
            val success = userCouponRepository.markAsUsed(it.id, savedOrder.id)
            if (!success) throw CoreException(CouponErrorCode.COUPON_ALREADY_USED)
        }

        return OrderInfo.from(savedOrder)
    }

    private fun validateCoupon(
        couponId: Long?,
        userId: Long,
    ): Pair<UserCoupon?, Coupon?> {
        if (couponId == null) return Pair(null, null)

        val userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId)
            ?: throw CoreException(CouponErrorCode.USER_COUPON_NOT_FOUND)
        userCoupon.validateUsableBy(userId)

        val coupon = couponRepository.findActiveByIdOrNull(couponId)
            ?: throw CoreException(CouponErrorCode.COUPON_NOT_FOUND)

        return Pair(userCoupon, coupon)
    }

    private fun calculateDiscount(
        coupon: Coupon?,
        originalAmount: Money,
    ): Money {
        if (coupon == null) return Money(0)
        coupon.validateApplicable(originalAmount)
        return coupon.calculateDiscount(originalAmount)
    }
}
