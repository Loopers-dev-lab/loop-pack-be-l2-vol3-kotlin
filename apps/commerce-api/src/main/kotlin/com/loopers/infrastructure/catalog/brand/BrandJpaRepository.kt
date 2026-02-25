package com.loopers.infrastructure.catalog.brand

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BrandJpaRepository : JpaRepository<BrandEntity, Long> {

    @Query("SELECT b FROM BrandEntity b WHERE b.deletedAt IS NULL")
    fun findAllActive(pageable: Pageable): List<BrandEntity>
}
