package com.loopers.domain.brand

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface BrandRepository {
    fun findById(id: Long): Brand?
    fun findAll(pageable: Pageable): Page<Brand>
    fun findAllByIds(ids: List<Long>): List<Brand>
    fun save(brand: Brand): Brand
}
