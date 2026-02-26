package com.loopers.domain.product

import com.loopers.domain.common.LikeCount
import com.loopers.domain.common.Money
import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import com.loopers.domain.common.StockQuantity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class ProductService(
    private val productRepository: ProductRepository,
) {

    fun createProduct(
        name: String,
        description: String?,
        price: Long,
        stockQuantity: Int,
        brandId: Long,
    ): Product {
        if (price <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "가격은 0보다 커야 합니다.")
        }
        return productRepository.save(
            Product(
                name = name,
                description = description,
                price = Money.of(price),
                likes = LikeCount.of(0),
                stockQuantity = StockQuantity.of(stockQuantity),
                brandId = brandId,
            ),
        )
    }

    fun getProducts(brandId: Long?, pageQuery: PageQuery): PageResult<Product> {
        return productRepository.findAll(brandId, pageQuery)
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
