package com.loopers.domain.brand

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface BrandRepository {
    fun save(brand: Brand): Brand
    fun findById(id: Long): Brand?
    fun findAll(pageable: Pageable): Page<Brand>
    fun findByIdIncludingDeleted(id: Long): Brand?
    fun findAllByIdIncludingDeleted(ids: List<Long>): List<Brand>
    fun existsById(id: Long): Boolean
    fun existsByName(name: String): Boolean
    fun existsByNameAndIdNot(name: String, id: Long): Boolean
}
