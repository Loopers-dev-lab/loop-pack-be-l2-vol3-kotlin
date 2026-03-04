package com.loopers.domain.brand

import com.loopers.domain.brand.vo.BrandName

interface BrandRepository {
    fun save(brand: Brand): Brand
    fun findById(id: Long): Brand?
    fun findByName(name: BrandName): Brand?
    fun existsByName(name: BrandName): Boolean
    fun findAllByStatus(status: BrandStatus): List<Brand>
    fun findAllByIds(ids: List<Long>): List<Brand>
}
