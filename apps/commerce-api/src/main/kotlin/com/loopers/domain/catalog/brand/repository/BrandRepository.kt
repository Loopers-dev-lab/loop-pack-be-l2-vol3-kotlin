package com.loopers.domain.catalog.brand.repository

import com.loopers.domain.PageResult
import com.loopers.domain.catalog.brand.model.Brand
import com.loopers.domain.common.vo.BrandId

interface BrandRepository {
    fun save(brand: Brand): Brand
    fun findById(id: BrandId): Brand?
    fun findAll(page: Int, size: Int): PageResult<Brand>
    fun existsByName(name: String): Boolean
}
