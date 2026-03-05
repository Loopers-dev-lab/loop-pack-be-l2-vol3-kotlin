package com.loopers.domain.product

import com.loopers.domain.common.Money
import com.loopers.support.common.PageQuery
import com.loopers.support.common.PageResult
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
        price: Money,
        stockQuantity: StockQuantity,
        brandId: Long,
    ): Product {
        return productRepository.save(
            Product.create(
                name = name,
                description = description,
                price = price,
                stockQuantity = stockQuantity,
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

    fun validateProductExists(productId: Long) {
        getProduct(productId)
    }

    fun updateProduct(
        product: Product,
        name: String,
        description: String?,
        price: Money,
        stockQuantity: StockQuantity,
        brandId: Long,
    ) {
        product.validateBrandChange(brandId)
        product.update(name, description, price, stockQuantity)
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

    fun incrementLikeCount(productId: Long) {
        productRepository.incrementLikeCount(productId)
    }

    fun decrementLikeCount(productId: Long) {
        productRepository.decrementLikeCount(productId)
    }

    fun delete(product: Product) {
        product.delete()
    }

    fun deleteAllByBrandId(brandId: Long) {
        val products = productRepository.findAllByBrandId(brandId)
        products.forEach { delete(it) }
    }
}
