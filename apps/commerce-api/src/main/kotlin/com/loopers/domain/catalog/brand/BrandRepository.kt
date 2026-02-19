package com.loopers.domain.catalog.brand

import com.loopers.domain.PageResult

interface BrandRepository {
    fun save(brand: Brand): Brand
    fun findById(id: Long): Brand?
    fun findAll(page: Int, size: Int): PageResult<Brand>
}
