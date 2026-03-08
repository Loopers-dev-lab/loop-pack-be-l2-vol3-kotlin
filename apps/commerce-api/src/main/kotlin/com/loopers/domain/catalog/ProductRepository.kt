package com.loopers.domain.catalog

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

interface ProductRepository {
    fun findById(id: Long): ProductModel?
    fun findByIdWithLock(id: Long): ProductModel?
    fun increaseLikeCount(id: Long): Boolean
    fun decreaseLikeCount(id: Long): Boolean
    fun findAll(pageable: Pageable): Slice<ProductModel>
    fun findAllByBrandId(brandId: Long): List<ProductModel>
    fun findAllByBrandId(brandId: Long, pageable: Pageable): Slice<ProductModel>
    fun search(sortType: ProductSortType, brandId: Long?, pageable: Pageable): Slice<ProductModel>
    fun save(product: ProductModel): ProductModel
}
