package com.loopers.domain.brand

interface BrandRepository {
    fun save(brand: Brand): Long
    fun findById(id: Long): Brand?
    fun existsByName(name: BrandName): Boolean
    fun findAll(): List<Brand>
    fun findAllActive(): List<Brand>
    fun findAllByIds(ids: Set<Long>): List<Brand>
}
