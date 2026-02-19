package com.loopers.application.order

import com.loopers.domain.catalog.CatalogService
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderCommand
import com.loopers.domain.order.OrderProductInfo
import com.loopers.domain.order.OrderService
import com.loopers.domain.point.UserPointService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderFacade(
    private val orderService: OrderService,
    private val catalogService: CatalogService,
    private val userPointService: UserPointService,
) {

    @Transactional
    fun createOrder(userId: Long, command: OrderCommand.CreateOrder): Order {
        val productIds = command.items.map { it.productId }
        val products = catalogService.getProductsForOrder(productIds)

        val stockDecrements = command.items.associate { it.productId to it.quantity }
        catalogService.decreaseStocks(stockDecrements)

        val productInfos = products.map { OrderProductInfo(it.id, it.name, it.price) }
        val savedOrder = orderService.createOrder(userId, productInfos, command)

        userPointService.usePoints(userId, savedOrder.totalPrice.toLong(), savedOrder.id)

        return savedOrder
    }
}
