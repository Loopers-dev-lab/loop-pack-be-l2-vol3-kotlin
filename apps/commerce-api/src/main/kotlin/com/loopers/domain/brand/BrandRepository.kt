package com.loopers.domain.brand

import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse

interface BrandRepository {
    fun save(brand: Brand, admin: String): Brand
    fun delete(brandId: Long, admin: String)
    fun findById(id: Long): Brand?
    fun findAll(): List<Brand>
    fun findAllByIdIn(ids: List<Long>): List<Brand>
    fun findAll(pageRequest: PageRequest): PageResponse<Brand>
}
