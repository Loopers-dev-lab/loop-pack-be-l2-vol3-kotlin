package com.loopers.application.order

import com.loopers.domain.catalog.CatalogService
import com.loopers.domain.order.OrderCommand
import com.loopers.domain.order.OrderProductInfo
import com.loopers.domain.order.OrderService
import com.loopers.domain.point.UserPointService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PlaceOrderUseCase(
    private val orderService: OrderService,
    private val catalogService: CatalogService,
    private val userPointService: UserPointService,
) {

    @Transactional
    fun execute(userId: Long, command: PlaceOrderCommand): OrderInfo {
        val productIds = command.items.map { it.productId }

        val products = catalogService.getProductsForOrder(productIds)

        val stockDecrements = command.items.associate { it.productId to it.quantity }
        catalogService.decreaseStocks(stockDecrements)

        val productInfos = products.map { OrderProductInfo(it.id, it.name, it.price) }
        val domainCommand = OrderCommand.CreateOrder(
            items = command.items.map { OrderCommand.CreateOrderItem(productId = it.productId, quantity = it.quantity) },
        )
        val orderDetail = orderService.createOrder(userId, productInfos, domainCommand)

        userPointService.usePoints(userId, orderDetail.order.totalPrice.toLong(), orderDetail.order.id)

        return OrderInfo.from(orderDetail)
    }
}
