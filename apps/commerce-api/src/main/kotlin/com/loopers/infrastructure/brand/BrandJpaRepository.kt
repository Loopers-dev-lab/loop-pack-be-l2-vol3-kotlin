package com.loopers.infrastructure.brand

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BrandJpaRepository : JpaRepository<BrandEntity, Long> {
    fun findByName(name: String): BrandEntity?
    fun existsByName(name: String): Boolean

    @Query("SELECT b FROM BrandEntity b WHERE b.deletedAt IS NULL")
    fun findAllActive(): List<BrandEntity>
}
