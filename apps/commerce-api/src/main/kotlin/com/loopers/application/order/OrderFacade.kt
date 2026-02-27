package com.loopers.application.order

import com.loopers.domain.brand.BrandService
import com.loopers.domain.order.OrderService
import com.loopers.domain.product.ProductService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderFacade(
    private val orderService: OrderService,
    private val productService: ProductService,
    private val brandService: BrandService,
) {
    @Transactional
    fun createOrder(userId: Long, items: List<OrderItemRequest>): OrderInfo {
        validateNoDuplicateProducts(items)

        val productIds = items.map { it.productId }
        val productMap = productService.findAllByIds(productIds).associateBy { it.id }

        items.forEach { item ->
            productMap[item.productId]
                ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다: ${item.productId}")
        }

        val brandIds = productMap.values.map { it.brandId }.distinct()
        val brandMap = brandService.findAllByIds(brandIds).associateBy { it.id }

        val orderItems = items.map { item ->
            val product = productMap[item.productId]
                ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다: ${item.productId}")
            product to item.quantity
        }

        val order = orderService.createOrder(
            userId = userId,
            orderItems = orderItems,
            brandNameResolver = { brandId ->
                brandMap[brandId]?.name
                    ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다: $brandId")
            },
        )

        return OrderInfo.from(order)
    }

    private fun validateNoDuplicateProducts(items: List<OrderItemRequest>) {
        val productIds = items.map { it.productId }
        if (productIds.size != productIds.distinct().size) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문에 중복된 상품이 포함되어 있습니다.")
        }
    }
}

data class OrderItemRequest(
    val productId: Long,
    val quantity: Int,
)
