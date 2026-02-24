package com.loopers.domain.brand

interface BrandRepository {
    fun find(id: Long): Brand?
    fun findAllByIds(ids: List<Long>): List<Brand>
    fun save(brand: Brand): Brand
}
