package com.loopers.application.order

import com.loopers.application.brand.BrandService
import com.loopers.domain.order.OrderCommand
import com.loopers.application.product.ProductService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class OrderFacade(
    private val orderService: OrderService,
    private val productService: ProductService,
    private val brandService: BrandService,
) {
    @Transactional
    fun createOrder(memberId: Long, command: OrderCommand.Create): OrderInfo {
        val productIds = command.items.map { it.productId }
        val products = productService.getProductsByIds(productIds)
        val productMap = products.associateBy { it.id }

        // Deduct stock for each item (SELECT FOR UPDATE)
        command.items.forEach { item ->
            productService.deductStock(item.productId, item.quantity)
        }

        // Get brand names for snapshot
        val brandIds = products.map { it.brandId }.distinct()
        val brandNames = brandIds.associateWith { brandId ->
            runCatching { brandService.getBrand(brandId) }.getOrNull()?.name ?: ""
        }

        return orderService.createOrder(memberId, productMap, brandNames, command.items)
            .let { OrderInfo.from(it) }
    }

    @Transactional(readOnly = true)
    fun getOrders(memberId: Long, startAt: ZonedDateTime, endAt: ZonedDateTime): List<OrderInfo> {
        return orderService.getOrdersByMember(memberId, startAt, endAt)
            .map { OrderInfo.from(it) }
    }

    @Transactional(readOnly = true)
    fun getOrder(memberId: Long, orderId: Long): OrderInfo {
        return orderService.getOrder(orderId, memberId)
            .let { OrderInfo.from(it) }
    }
}
