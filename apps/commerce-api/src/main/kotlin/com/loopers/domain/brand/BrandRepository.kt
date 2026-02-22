package com.loopers.domain.brand

interface BrandRepository {
    fun findByName(name: String): BrandModel?
    fun save(brand: BrandModel): BrandModel
}
