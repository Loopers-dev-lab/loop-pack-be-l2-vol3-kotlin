package com.loopers.domain.brand

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface BrandRepository {
    fun save(brand: BrandModel): BrandModel
    fun findByIdAndDeletedAtIsNull(id: Long): BrandModel?
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<BrandModel>
    fun existsByNameAndDeletedAtIsNull(name: String): Boolean
    fun findAllByIdInAndDeletedAtIsNull(ids: List<Long>): List<BrandModel>
}
