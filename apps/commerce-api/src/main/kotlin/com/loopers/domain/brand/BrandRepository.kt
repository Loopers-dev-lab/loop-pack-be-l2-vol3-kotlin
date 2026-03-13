package com.loopers.domain.brand

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface BrandRepository {
    fun save(brand: Brand): Brand
    fun findByIdAndDeletedAtIsNull(id: Long): Brand?
    fun findAllByIdInAndDeletedAtIsNull(ids: List<Long>): List<Brand>
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<Brand>
}
