package com.loopers.domain.brand

interface BrandRepository {
    fun findById(id: Long): BrandModel?
    fun findByName(name: String): BrandModel?
    fun save(brand: BrandModel): BrandModel
}
