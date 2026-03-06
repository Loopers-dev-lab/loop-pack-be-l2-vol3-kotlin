package com.loopers.domain.order

import com.loopers.domain.product.ProductModel
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class OrderService(
    private val orderRepository: OrderRepository,
) {
    @Transactional(readOnly = true)
    fun findById(id: Long): OrderModel {
        return orderRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 주문입니다: $id")
    }

    @Transactional(readOnly = true)
    fun findByIdAndUserId(id: Long, userId: Long): OrderModel {
        val order = findById(id)
        if (order.userId != userId) {
            throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 주문입니다: $id")
        }
        return order
    }

    @Transactional(readOnly = true)
    fun findAll(pageable: Pageable): Page<OrderModel> {
        return orderRepository.findAllByDeletedAtIsNull(pageable)
    }

    @Transactional(readOnly = true)
    fun findByUserIdAndDateRange(
        userId: Long,
        startAt: LocalDate,
        endAt: LocalDate,
        pageable: Pageable,
    ): Page<OrderModel> {
        return orderRepository.findAllByUserIdAndCreatedAtBetween(userId, startAt, endAt, pageable)
    }

    /**
     * 주문을 생성한다.
     * - 재고 확인 및 차감 (Product.decreaseStock)
     * - 스냅샷 저장
     * - 원자성 보장 (일부 재고 부족 시 전체 실패)
     *
     * 주의: 재고 차감은 Product Entity의 decreaseStock()에 위임한다.
     * OrderService는 흐름을 조율할 뿐, 재고 검증 로직 자체를 갖지 않는다.
     */
    @Transactional
    fun createOrder(
        userId: Long,
        orderItems: List<Pair<ProductModel, Int>>,
        brandNameResolver: (Long) -> String,
    ): OrderModel {
        val order = OrderModel(userId = userId)

        orderItems.forEach { (product, quantity) ->
            product.decreaseStock(quantity)
            val item = OrderItemModel(
                order = order,
                productId = product.id,
                productName = product.name,
                brandName = brandNameResolver(product.brandId),
                price = product.price,
                quantity = quantity,
            )
            order.addItem(item)
        }

        return orderRepository.save(order)
    }
}
