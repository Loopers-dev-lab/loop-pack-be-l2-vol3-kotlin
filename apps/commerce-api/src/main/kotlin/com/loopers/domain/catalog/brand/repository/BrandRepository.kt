package com.loopers.domain.catalog.brand.repository

import com.loopers.domain.PageResult
import com.loopers.domain.catalog.brand.model.Brand

interface BrandRepository {
    fun save(brand: Brand): Brand
    fun findById(id: Long): Brand?
    fun findAll(page: Int, size: Int): PageResult<Brand>
}
