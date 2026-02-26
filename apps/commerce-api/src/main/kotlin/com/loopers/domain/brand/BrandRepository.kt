package com.loopers.domain.brand

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface BrandRepository {
    fun save(brand: Brand): Brand
    fun findById(id: Long): Brand?
    fun findByIds(ids: List<Long>): List<Brand>
    fun findAll(): List<Brand>
    fun findAll(pageable: Pageable): Page<Brand>
}
