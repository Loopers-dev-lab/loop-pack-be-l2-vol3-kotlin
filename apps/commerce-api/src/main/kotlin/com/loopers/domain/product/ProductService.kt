package com.loopers.domain.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class ProductService(
    private val productRepository: ProductRepository,
) {

    fun getProducts(brandId: Long?, pageable: Pageable): Page<Product> {
        return productRepository.findAll(brandId, pageable)
    }

    fun getProduct(productId: Long): Product {
        return productRepository.findById(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
    }

    fun increaseLikeCount(product: Product) {
        product.increaseLikeCount()
    }

    fun decreaseLikeCount(product: Product) {
        product.decreaseLikeCount()
    }

    fun getProductsByIds(ids: List<Long>): List<Product> {
        return productRepository.findAllByIds(ids)
    }

    fun getProductsForOrder(productIds: List<Long>): List<Product> {
        val products = getProductsByIds(productIds)
        val foundIds = products.map { it.id }.toSet()
        val missingIds = productIds.filter { it !in foundIds }
        if (missingIds.isNotEmpty()) {
            throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다: $missingIds")
        }
        return products
    }

    fun deductStocks(products: Map<Long, Product>, requests: List<StockDeductionRequest>) {
        for (request in requests) {
            products.getValue(request.productId).deductStock(request.quantity)
        }
    }
}
