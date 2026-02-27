package com.loopers.application.order

import com.loopers.application.brand.BrandService
import com.loopers.application.product.ProductService
import com.loopers.domain.order.OrderItemCommand
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderFacade(
    private val orderService: OrderService,
    private val productService: ProductService,
    private val brandService: BrandService,
) {

    @Transactional
    fun createOrder(userId: Long, criteria: List<OrderItemCriteria>): OrderResultInfo {
        val productIds = criteria.map { it.productId }
        val products = productService.getProductsWithLock(productIds)

        val reservation = productService.reserveStock(products, criteria)

        val brandIds = reservation.reservedProducts.map { it.brandId }.distinct()
        val brandMap = brandService.getBrandsIncludingDeleted(brandIds).associateBy { it.id }

        val orderItemCommands = reservation.reservedProducts.map { reserved ->
            OrderItemCommand(
                productId = reserved.productId,
                productName = reserved.productName,
                brandName = brandMap[reserved.brandId]?.name ?: "-",
                quantity = reserved.quantity,
                unitPrice = reserved.unitPrice,
            )
        }

        val order = orderService.createOrder(userId, orderItemCommands)

        val excludedItems = reservation.failedReservations.map {
            ExcludedItemInfo(it.productId, it.reason)
        }
        return OrderResultInfo.of(order, excludedItems)
    }
}
