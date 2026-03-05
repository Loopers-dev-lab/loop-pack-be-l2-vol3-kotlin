package com.loopers.application.user.order

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Quantity
import com.loopers.domain.order.IdempotencyKey
import com.loopers.domain.order.OrderDomainService
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.order.OrderSnapshot
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStockRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderCreateUseCase(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val productStockRepository: ProductStockRepository,
    private val brandRepository: BrandRepository,
) {
    @Transactional
    fun create(command: OrderCreateCommand): OrderResult.Created {
        val idempotencyKey = IdempotencyKey(command.idempotencyKey)

        if (orderRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw CoreException(ErrorType.ORDER_IDEMPOTENCY_KEY_DUPLICATE)
        }

        if (command.items.any { it.quantity <= 0 }) {
            throw CoreException(ErrorType.INVALID_QUANTITY)
        }

        val mergedItems = command.items
            .groupBy { it.productId }
            .map { (productId, items) ->
                OrderCreateCommand.Item(productId, items.sumOf { it.quantity })
            }

        val productIds = mergedItems.map { it.productId }

        val products = productRepository.findAllByIdIn(productIds)
        validateProducts(productIds, products)

        val brandIds = products.map { it.brandId }.distinct()
        val brands = brandRepository.findAllByIdIn(brandIds)
        validateBrands(brandIds, brands)

        val stocks = productStockRepository.findAllByProductIdIn(productIds)

        val productMap = products.associateBy { it.id!! }
        val brandMap = brands.associateBy { it.id!! }
        val stockMap = stocks.associateBy { it.productId }

        val orderItemRequests = mergedItems.map { item ->
            val product = productMap[item.productId]!!
            val brand = brandMap[product.brandId]!!
            val stock = stockMap[item.productId]
                ?: throw CoreException(ErrorType.PRODUCT_STOCK_NOT_FOUND)

            OrderDomainService.OrderItemRequest(
                productStock = stock,
                snapshot = OrderSnapshot(
                    productId = product.id!!,
                    productName = product.name,
                    brandId = brand.id!!,
                    brandName = brand.name.value,
                    regularPrice = product.regularPrice,
                    sellingPrice = product.sellingPrice,
                    thumbnailUrl = product.thumbnailUrl,
                ),
                quantity = Quantity(item.quantity),
            )
        }

        val domainResult = OrderDomainService.createOrder(
            userId = command.userId,
            idempotencyKey = idempotencyKey,
            orderItemRequests = orderItemRequests,
        )

        val savedOrder = orderRepository.save(domainResult.order)
        productStockRepository.saveAll(domainResult.decreasedStocks)

        return OrderResult.Created.from(savedOrder)
    }

    private fun validateProducts(requestedIds: List<Long>, products: List<Product>) {
        if (products.size != requestedIds.size) {
            throw CoreException(ErrorType.PRODUCT_NOT_FOUND)
        }
        if (products.any { it.status != Product.Status.ACTIVE }) {
            throw CoreException(ErrorType.PRODUCT_NOT_FOUND)
        }
    }

    private fun validateBrands(requestedIds: List<Long>, brands: List<Brand>) {
        if (brands.size != requestedIds.size) {
            throw CoreException(ErrorType.PRODUCT_NOT_FOUND)
        }
        if (brands.any { it.status != Brand.Status.ACTIVE }) {
            throw CoreException(ErrorType.PRODUCT_NOT_FOUND)
        }
    }
}
