package com.loopers.application.order

import com.loopers.domain.brand.BrandService
import com.loopers.domain.order.OrderItemCommand
import com.loopers.domain.order.OrderService
import com.loopers.domain.product.ProductService
import com.loopers.domain.product.StockDeductionRequest
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
        val productIds = items.map { it.productId }

        // 재고 차감 동시성 제어: 락 획득 후 트랜잭션 커밋 시 자동 해제
        stockLockManager.acquireLocksForTransaction(productIds)

        // 락 획득 후 상품 재조회 (최신 재고 상태 보장) + 존재 검증
        val products = productService.getProductsForOrder(productIds)
        val productMap = products.associateBy { it.id }

        val brandMap = brandService.getBrandsByIds(
            products.map { it.brandId }.distinct(),
        ).associateBy { it.id }

        val deductionRequests = items.map { StockDeductionRequest(it.productId, it.quantity) }
        productService.deductStocks(productMap, deductionRequests)

        // cross-domain 스냅샷 조립은 application 레이어에 유지
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
