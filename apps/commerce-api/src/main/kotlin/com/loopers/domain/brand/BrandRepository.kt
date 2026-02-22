package com.loopers.domain.brand

interface BrandRepository {
    fun find(id: Long): Brand?
    fun save(brand: Brand): Brand
}
