package com.loopers.domain.catalog

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

interface ProductRepository {
    fun findById(id: Long): ProductModel?
    fun decreaseStock(id: Long, quantity: Int): Boolean
    fun increaseStock(id: Long, quantity: Int): Boolean
    fun findAll(pageable: Pageable): Slice<ProductModel>
    fun findAllByBrandId(brandId: Long): List<ProductModel>
    fun findAllByBrandId(brandId: Long, pageable: Pageable): Slice<ProductModel>
    fun save(product: ProductModel): ProductModel
}
