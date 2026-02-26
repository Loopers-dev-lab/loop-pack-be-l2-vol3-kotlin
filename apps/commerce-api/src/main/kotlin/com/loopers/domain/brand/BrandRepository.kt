package com.loopers.domain.brand

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface BrandRepository {
    fun save(brand: BrandModel): BrandModel

    fun findById(id: Long): BrandModel?

    fun findAll(pageable: Pageable): Page<BrandModel>
}
