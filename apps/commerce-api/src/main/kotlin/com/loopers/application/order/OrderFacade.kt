package com.loopers.application.order

import com.loopers.application.brand.BrandService
import com.loopers.application.product.ProductService
import com.loopers.domain.order.ExcludedItem
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItemCommand
import com.loopers.domain.order.OrderResult
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
    fun createOrder(userId: Long, commands: List<OrderItemCommand>): OrderResult {
        val productIds = commands.map { it.productId }
        val products = productService.getProductsWithLock(productIds)
        val productMap = products.associateBy { it.id }

        val excludedItems = mutableListOf<ExcludedItem>()
        val order = Order(userId = userId)

        for (command in commands) {
            val product = productMap[command.productId]

            if (product == null) {
                excludedItems.add(ExcludedItem(command.productId, "존재하지 않는 상품입니다."))
                continue
            }

            if (!product.hasEnoughStock(command.quantity)) {
                excludedItems.add(ExcludedItem(command.productId, "재고가 부족합니다. 현재 재고: ${product.stock}"))
                continue
            }

            product.decreaseStock(command.quantity)

            val brandName = brandService.getBrandIncludingDeleted(product.brandId)?.name ?: "알 수 없는 브랜드"

            order.addItem(
                productId = product.id,
                productName = product.name,
                brandName = brandName,
                quantity = command.quantity,
                unitPrice = product.price,
            )
        }

        if (order.orderItems.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 가능한 상품이 없습니다.")
        }

        val savedOrder = orderService.createOrder(order)
        return OrderResult(order = savedOrder, excludedItems = excludedItems)
    }
}
