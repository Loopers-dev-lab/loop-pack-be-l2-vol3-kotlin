package com.loopers.application.order

import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.coupon.repository.CouponRepository
import com.loopers.domain.coupon.repository.IssuedCouponRepository
import com.loopers.domain.order.OrderProductData
import com.loopers.domain.order.model.Order
import com.loopers.domain.order.repository.OrderItemRepository
import com.loopers.domain.order.repository.OrderRepository
import com.loopers.domain.common.vo.Quantity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Component
class PlaceOrderUseCase(
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val couponRepository: CouponRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
) {

    @Transactional
    fun execute(userId: Long, command: PlaceOrderCommand): OrderInfo {
        if (command.items.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 항목이 비어있습니다.")
        }

        val products = productRepository.findAllByIdsForUpdate(command.items.map { ProductId(it.productId) })
        if (products.size != command.items.size) {
            throw CoreException(ErrorType.BAD_REQUEST, "존재하지 않는 상품이 포함되어 있습니다.")
        }
        val productMap = products.associateBy { it.id }

        val orderItemInputs = command.items.map { item ->
            val product = productMap[ProductId(item.productId)]
                ?: throw CoreException(ErrorType.BAD_REQUEST, "존재하지 않는 상품이 포함되어 있습니다.")
            if (!product.isAvailableForOrder()) {
                throw CoreException(ErrorType.BAD_REQUEST, "주문 가능한 상태가 아닌 상품이 포함되어 있습니다.")
            }
            val quantity = Quantity(item.quantity)
            product.decreaseStock(quantity)
            OrderProductData(product.id, product.name, product.price) to quantity
        }
        productRepository.saveAll(products)

        // 쿠폰 할인 계산
        val originalPrice = orderItemInputs.fold(Money(BigDecimal.ZERO)) { acc, (data, qty) ->
            acc + (data.price * qty.value)
        }
        var discountAmount = Money(BigDecimal.ZERO)
        var refCouponId: Long? = null

        if (command.couponId != null) {
            val issuedCoupon = issuedCouponRepository.findByIdForUpdate(command.couponId)
                ?: throw CoreException(ErrorType.BAD_REQUEST, "존재하지 않는 발급 쿠폰입니다.")

            if (!issuedCoupon.isOwnedBy(UserId(userId))) {
                throw CoreException(ErrorType.BAD_REQUEST, "본인 소유의 쿠폰이 아닙니다.")
            }

            if (!issuedCoupon.isAvailable()) {
                throw CoreException(ErrorType.BAD_REQUEST, "사용할 수 없는 쿠폰입니다.")
            }

            val coupon = couponRepository.findById(issuedCoupon.refCouponId)
                ?: throw CoreException(ErrorType.BAD_REQUEST, "쿠폰 정보를 찾을 수 없습니다.")

            if (coupon.isExpired()) {
                throw CoreException(ErrorType.BAD_REQUEST, "만료된 쿠폰입니다.")
            }

            val minOrderAmount = coupon.minOrderAmount
            if (minOrderAmount != null && originalPrice.value < minOrderAmount.value) {
                throw CoreException(ErrorType.BAD_REQUEST, "최소 주문 금액을 충족하지 않습니다.")
            }

            discountAmount = coupon.calculateDiscount(originalPrice)
            issuedCoupon.use()
            issuedCouponRepository.save(issuedCoupon)
            refCouponId = command.couponId
        }

        val order = Order.create(UserId(userId), orderItemInputs, discountAmount, refCouponId)
        val savedOrder = orderRepository.save(order)

        order.assignOrderIdToItems(savedOrder.id)
        val savedItems = orderItemRepository.saveAll(order.items)

        return OrderInfo.from(OrderDetail(savedOrder, savedItems))
    }
}
