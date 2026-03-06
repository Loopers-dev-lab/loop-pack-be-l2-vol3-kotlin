package com.loopers.domain.product

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ProductRepository {
    fun save(product: ProductModel): ProductModel
    fun findByIdAndDeletedAtIsNull(id: Long): ProductModel?
    fun findAllByDeletedAtIsNull(brandId: Long?, pageable: Pageable): Page<ProductModel>
    fun findAllByIdInAndDeletedAtIsNull(ids: List<Long>): List<ProductModel>
    fun findAllByIdsForUpdate(ids: List<Long>): List<ProductModel>
}
