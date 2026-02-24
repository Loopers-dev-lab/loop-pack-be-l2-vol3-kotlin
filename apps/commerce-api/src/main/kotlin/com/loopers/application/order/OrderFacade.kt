package com.loopers.application.order

import com.loopers.domain.brand.BrandService
import com.loopers.domain.order.OrderItemCommand
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
    fun placeOrder(userId: Long, items: List<OrderPlaceCommand>) {
        validateOrderItems(items)

        val productIds = items.map { it.productId }
        val products = productService.getProductsByIds(productIds)
        if (products.size != productIds.size) {
            throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        }
        val productMap = products.associateBy { it.id }

        val brandIds = products.map { it.brandId }.distinct()
        val brandMap = brandIds.associateWith { brandService.getBrand(it) }

        for (item in items) {
            productService.deductStock(productMap.getValue(item.productId), item.quantity)
        }

        val orderItemCommands = items.map { item ->
            val product = productMap.getValue(item.productId)
            val brand = brandMap.getValue(product.brandId)
            OrderItemCommand(
                productId = item.productId,
                quantity = item.quantity,
                productName = product.name,
                productPrice = product.price,
                brandName = brand.name,
            )
        }

        orderService.createOrder(userId, orderItemCommands)
    }

    private fun validateOrderItems(items: List<OrderPlaceCommand>) {
        if (items.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 항목이 비어있습니다.")
        }
        if (items.any { it.quantity <= 0 }) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 수량은 0보다 커야 합니다.")
        }
        val productIds = items.map { it.productId }
        if (productIds.size != productIds.toSet().size) {
            throw CoreException(ErrorType.BAD_REQUEST, "중복된 상품이 포함되어 있습니다.")
        }
    }
}
