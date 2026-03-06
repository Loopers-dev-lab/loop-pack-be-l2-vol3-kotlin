package com.loopers.domain.product

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ProductRepository {
    fun save(product: Product): Product
    fun findByIdAndDeletedAtIsNull(id: Long): Product?
    fun findByIdWithPessimisticLock(id: Long): Product?
    fun findAllByCondition(brandId: Long?, sort: ProductSort, pageable: Pageable): Page<Product>
    fun increaseLikeCount(productId: Long)
    fun decreaseLikeCount(productId: Long)
    fun softDeleteByBrandId(brandId: Long)
}
