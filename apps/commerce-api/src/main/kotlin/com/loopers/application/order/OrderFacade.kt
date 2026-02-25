package com.loopers.application.order

import com.loopers.domain.catalog.brand.BrandService
import com.loopers.domain.catalog.product.ProductRepository
import com.loopers.domain.catalog.product.ProductService
import com.loopers.domain.order.OrderItem
import com.loopers.domain.order.OrderService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderFacade(
    private val orderService: OrderService,
    private val productService: ProductService,
    private val brandService: BrandService,
    private val productRepository: ProductRepository,
) {

    @Transactional
    fun placeOrder(userId: Long, cmd: PlaceOrderCommand): OrderResult {
        if (cmd.items.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 항목이 비어있을 수 없습니다.")
        }

        // 1. 모든 상품 조회 및 페어링 (즉시 페어링하여 수량 불일치로 인한 묵시적 누락 방지)
        val itemWithProducts = cmd.items.map { item -> item to productService.getById(item.productId) }

        if (itemWithProducts.size != cmd.items.size) {
            throw CoreException(ErrorType.BAD_REQUEST, "일부 상품이 조회되지 않았습니다.")
        }

        // 2. 모든 재고 검증 (fail-fast: 부분 처리 없이 전체 실패)
        itemWithProducts.forEach { (item, product) ->
            product.validateStock(item.quantity)
        }

        // 3. 모든 재고 차감
        itemWithProducts.forEach { (item, product) ->
            product.decrementStock(item.quantity)
            productRepository.save(product)
        }

        // 4. 브랜드 조회 및 주문 항목 스냅샷 생성
        val orderItems = itemWithProducts.map { (item, product) ->
            val brand = brandService.getById(product.brandId)
            OrderItem(
                orderId = 0L,
                productId = product.id,
                productName = product.name,
                brandId = brand.id,
                brandName = brand.name,
                price = product.price,
                quantity = item.quantity,
            )
        }

        // 5. 주문 생성 및 저장
        val order = orderService.createOrder(userId = userId, items = orderItems)
        return OrderResult.from(order)
    }
}
