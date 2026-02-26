package com.loopers.domain.brand

interface BrandRepository {
    fun findByIdOrNull(id: Long): Brand?
    fun findAllByIds(ids: List<Long>): List<Brand>
    fun findAllActive(): List<Brand>
    fun findAll(): List<Brand>
    fun existsActiveByName(name: String): Boolean
    fun save(brand: Brand): Brand
}
