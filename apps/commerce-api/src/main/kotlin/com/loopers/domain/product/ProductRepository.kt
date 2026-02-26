package com.loopers.domain.product

import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult

interface ProductRepository {
    fun save(product: ProductModel): ProductModel

    fun findById(id: Long): ProductModel?

    fun findByIdWithLock(id: Long): ProductModel?

    fun findAll(pageQuery: PageQuery): PageResult<ProductModel>

    fun findAllByBrandId(brandId: Long, pageQuery: PageQuery): PageResult<ProductModel>

    fun findAllByIdIn(ids: List<Long>): List<ProductModel>

    fun findActiveProducts(condition: ProductSearchCondition): List<ProductModel>

    fun findAllByBrandIdAndStatus(brandId: Long, status: ProductStatus): List<ProductModel>
}
