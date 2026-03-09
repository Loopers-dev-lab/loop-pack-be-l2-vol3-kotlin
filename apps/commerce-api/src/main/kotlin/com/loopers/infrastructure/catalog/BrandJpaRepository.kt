package com.loopers.infrastructure.catalog

import com.loopers.domain.catalog.BrandModel
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository

interface BrandJpaRepository : JpaRepository<BrandModel, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): BrandModel?
    fun findByNameAndDeletedAtIsNull(name: String): BrandModel?
    fun findAllByDeletedAtIsNull(pageable: Pageable): Slice<BrandModel>
}
