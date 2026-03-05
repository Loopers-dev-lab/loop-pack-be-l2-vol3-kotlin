package com.loopers.infrastructure.brand

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface BrandJpaRepository : JpaRepository<BrandEntity, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): BrandEntity?
    fun findAllByDeletedAtIsNull(): List<BrandEntity>
    fun findAllByIdInAndDeletedAtIsNull(ids: List<Long>): List<BrandEntity>
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<BrandEntity>
}
