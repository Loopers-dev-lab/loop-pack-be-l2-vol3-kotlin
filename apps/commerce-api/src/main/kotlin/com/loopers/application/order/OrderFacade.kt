package com.loopers.application.order

import com.loopers.application.brand.BrandService
import com.loopers.application.product.ProductService
import com.loopers.domain.order.ExcludedItem
import com.loopers.domain.order.OrderItemCommand
import com.loopers.domain.order.OrderResult
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

        // 1. 재고 예약 (Product.reserve()로 재고 검증+차감)
        val reservation = productService.reserveStock(products, criteria)

        // 2. 브랜드 정보 조회 & OrderItemCommand 조립
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

        // 3. 주문 생성 & 저장
        val order = orderService.createOrder(userId, orderItemCommands)

        // 4. 결과 조합
        val excludedItems = reservation.failedReservations.map {
            ExcludedItem(it.productId, it.reason)
        }
        return OrderResultInfo.from(OrderResult(order, excludedItems))
    }
}
