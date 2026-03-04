package com.loopers.application.order

import com.loopers.domain.coupon.UserCouponRepository
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 교차 집계: Order + Product(재고 복원) + UserCoupon(쿠폰 복원)
 * MSA 분리 시 Product 재고 복원 → Product Service API 호출 + Saga 보상 트랜잭션 필요
 */
@Component
class CancelOrderUseCase(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val userCouponRepository: UserCouponRepository,
) {
    @Transactional
    fun cancel(userId: Long, orderId: Long) {
        val order = orderRepository.findByIdForUpdate(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다: $orderId")

        order.assertOwnedBy(userId)

        val cancelledOrder = order.cancel()
        orderRepository.save(cancelledOrder)

        cancelledOrder.items.forEach { item ->
            val affected = productRepository.increaseStock(item.refProductId, item.quantity)
            if (affected == 0) {
                throw CoreException(
                    ErrorType.NOT_FOUND,
                    "재고 복구에 실패했습니다. 상품 ID: ${item.refProductId}",
                )
            }
        }

        if (cancelledOrder.hasCoupon()) {
            val userCouponId = requireNotNull(cancelledOrder.refUserCouponId)
            val userCoupon = userCouponRepository.findByIdForUpdate(userCouponId)
                ?: throw CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다: $userCouponId")
            val restoredCoupon = userCoupon.restore()
            userCouponRepository.save(restoredCoupon)
        }
    }
}
