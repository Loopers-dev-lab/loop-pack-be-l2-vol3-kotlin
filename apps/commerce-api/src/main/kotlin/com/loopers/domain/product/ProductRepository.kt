package com.loopers.domain.product

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ProductRepository {
    fun save(product: ProductModel): ProductModel

    fun findById(id: Long): ProductModel?

    fun findByIdWithLock(id: Long): ProductModel?

    fun findAll(pageable: Pageable): Page<ProductModel>

    fun findAllByBrandId(brandId: Long, pageable: Pageable): Page<ProductModel>

    fun findAllByIdIn(ids: List<Long>): List<ProductModel>

    fun findActiveProducts(condition: ProductSearchCondition): List<ProductModel>

    fun findAllByBrandIdAndStatus(brandId: Long, status: ProductStatus): List<ProductModel>
}

data class ProductSearchCondition(
    val brandId: Long? = null,
    val sort: ProductSort = ProductSort.LATEST,
    val size: Int = 20,
    val cursor: Map<String, Any>? = null,
)

enum class ProductSort {
    LATEST,
    PRICE_ASC,
    LIKES_DESC,
}
