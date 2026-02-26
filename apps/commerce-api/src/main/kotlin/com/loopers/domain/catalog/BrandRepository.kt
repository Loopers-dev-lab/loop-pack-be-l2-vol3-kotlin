package com.loopers.domain.catalog

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

interface BrandRepository {
    fun findById(id: Long): BrandModel?
    fun findByName(name: String): BrandModel?
    fun findAll(pageable: Pageable): Slice<BrandModel>
    fun save(brand: BrandModel): BrandModel
}
