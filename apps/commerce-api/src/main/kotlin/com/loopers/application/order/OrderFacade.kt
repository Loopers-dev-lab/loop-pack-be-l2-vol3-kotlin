package com.loopers.application.order

import com.loopers.domain.brand.BrandService
import com.loopers.domain.order.OrderItemCommand
import com.loopers.domain.order.OrderService
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
        val order = orderService.getOrder(orderId)
        if (order.userId != userId) {
            throw CoreException(ErrorType.FORBIDDEN, "다른 사용자의 주문을 조회할 수 없습니다.")
        }
        val items = orderService.getOrderItems(orderId)
        return OrderDetailInfo.from(order, items)
    }

    @Transactional(readOnly = true)
    fun getOrders(userId: Long, startAt: LocalDateTime, endAt: LocalDateTime): List<OrderInfo> {
        if (startAt.isAfter(endAt)) {
            throw CoreException(ErrorType.BAD_REQUEST, "시작일이 종료일보다 클 수 없습니다.")
        }
        val orders = orderService.getOrders(userId, startAt, endAt)
        return orders.map { OrderInfo.from(it) }
    }

    @Transactional
    fun placeOrder(userId: Long, items: List<OrderPlaceCommand>) {
        validateOrderItems(items)

        val productIds = items.map { it.productId }

        // 재고 차감 동시성 제어: 락 획득 후 트랜잭션 커밋 시 자동 해제
        stockLockManager.acquireLocksForTransaction(productIds)

        // 락 획득 후 상품 재조회 (최신 재고 상태 보장)
        val products = productService.getProductsByIds(productIds)
        if (products.size != productIds.size) {
            throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
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
