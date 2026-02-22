package com.loopers.infrastructure.brand

import org.springframework.data.jpa.repository.JpaRepository

interface BrandJpaRepository : JpaRepository<BrandEntity, Long> {
    fun findAllByDeletedAtIsNull(): List<BrandEntity>
    fun existsByNameAndDeletedAtIsNull(name: String): Boolean
}
