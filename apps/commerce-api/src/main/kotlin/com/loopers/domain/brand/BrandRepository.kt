package com.loopers.domain.brand

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface BrandRepository {

    fun findById(id: Long): Brand

    fun findAll(pageable: Pageable): Page<Brand>

    fun existsByName(name: String): Boolean

    fun save(brand: Brand): Brand
}
