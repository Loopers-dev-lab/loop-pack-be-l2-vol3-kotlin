package com.loopers.application.order

import com.loopers.domain.brand.BrandService
import com.loopers.domain.order.OrderItemCommand
import com.loopers.domain.order.OrderService
import com.loopers.domain.order.StockLockManager
import com.loopers.domain.product.ProductService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class OrderFacade(
    private val orderService: OrderService,
    private val productService: ProductService,
    private val brandService: BrandService,
    private val stockLockManager: StockLockManager,
) {

    @Transactional(readOnly = true)
    fun getOrder(userId: Long, orderId: Long): OrderDetailInfo {
        val order = orderService.getOrder(userId, orderId)
        return OrderDetailInfo.from(order)
    }

    @Transactional(readOnly = true)
    fun getOrders(userId: Long, startAt: LocalDateTime, endAt: LocalDateTime): List<OrderInfo> {
        val orders = orderService.getOrders(userId, startAt, endAt)
        return orders.map { OrderInfo.from(it) }
    }

    @Transactional
    fun placeOrder(userId: Long, items: List<OrderPlaceCommand>) {
        val productIds = items.map { it.productId }.distinct()

        // 재고 차감 동시성 제어: 락 획득 후 트랜잭션 커밋 시 자동 해제
        stockLockManager.acquireLocksForTransaction(productIds)

        // 락 획득 후 상품 재조회 (최신 재고 상태 보장)
        val products = productService.getProductsByIds(productIds)
        val foundIds = products.map { it.id }.toSet()
        val missingIds = productIds.filter { it !in foundIds }
        if (missingIds.isNotEmpty()) {
            throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다: $missingIds")
        }
        val productMap = products.associateBy { it.id }

        val brandIds = products.map { it.brandId }.distinct()
        val brands = brandService.getBrandsByIds(brandIds)
        val brandMap = brands.associateBy { it.id }

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
}
