package com.loopers.domain.product

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ProductRepository {
    fun save(product: ProductModel): ProductModel
    fun findById(id: Long): ProductModel?
    fun findAll(pageable: Pageable): Page<ProductModel>
    fun findAllByBrandId(brandId: Long, pageable: Pageable): Page<ProductModel>
    fun findAllByBrandId(brandId: Long): List<ProductModel>
    fun existsBy(brandId: Long, name: Name): Boolean
    fun incrementLikeCount(id: Long)
    fun decrementLikeCount(id: Long)
}
