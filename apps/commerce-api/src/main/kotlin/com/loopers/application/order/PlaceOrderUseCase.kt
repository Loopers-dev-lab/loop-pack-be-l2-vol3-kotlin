package com.loopers.application.order

import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.common.vo.CouponId
import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.coupon.repository.CouponRepository
import com.loopers.domain.coupon.repository.IssuedCouponRepository
import com.loopers.domain.coupon.service.CouponValidator
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
    private val couponValidator: CouponValidator,
) {

    @Transactional
    fun execute(userId: Long, command: PlaceOrderCommand): OrderInfo {
        if (command.items.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 항목이 비어있습니다.")
        }

        // 1. Product 락 + 검증 + 재고 차감
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

        val originalPrice = orderItemInputs.fold(Money(BigDecimal.ZERO)) { acc, (data, qty) ->
            acc + (data.price * qty.value)
        }
        productRepository.saveAll(products)

        // 2. IssuedCoupon 락 + 검증 + 할인 계산 + use()
        var discountAmount = Money(BigDecimal.ZERO)
        var refCouponId: CouponId? = null

        if (command.issuedCouponId != null) {
            val issuedCoupon = issuedCouponRepository.findByIdForUpdate(command.issuedCouponId)
                ?: throw CoreException(ErrorType.BAD_REQUEST, "존재하지 않는 발급 쿠폰입니다.")

            val coupon = couponRepository.findById(issuedCoupon.refCouponId)
                ?: throw CoreException(ErrorType.BAD_REQUEST, "쿠폰 정보를 찾을 수 없습니다.")

            couponValidator.validateForOrder(issuedCoupon, coupon, UserId(userId), originalPrice)

            discountAmount = coupon.calculateDiscount(originalPrice)
            issuedCoupon.use()
            issuedCouponRepository.save(issuedCoupon)
            refCouponId = coupon.id
        }

        // 3. 주문 생성 + 저장
        val order = Order.create(UserId(userId), orderItemInputs, discountAmount, refCouponId)
        val savedOrder = orderRepository.save(order)

        order.assignOrderIdToItems(savedOrder.id)
        val savedItems = orderItemRepository.saveAll(order.items)

        return OrderInfo.from(OrderDetail(savedOrder, savedItems))
    }
}
