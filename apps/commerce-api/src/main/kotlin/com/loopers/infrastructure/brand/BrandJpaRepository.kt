package com.loopers.infrastructure.brand

import com.loopers.domain.brand.BrandModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface BrandJpaRepository : JpaRepository<BrandModel, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): BrandModel?
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<BrandModel>
    fun existsByNameAndDeletedAtIsNull(name: String): Boolean
    fun findAllByIdInAndDeletedAtIsNull(ids: List<Long>): List<BrandModel>
}
