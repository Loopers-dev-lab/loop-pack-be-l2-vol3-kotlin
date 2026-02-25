package com.loopers.infrastructure.brand

import org.springframework.data.jpa.repository.JpaRepository

interface BrandJpaRepository : JpaRepository<BrandEntity, Long> {
    fun findByName(name: String): BrandEntity?
    fun existsByName(name: String): Boolean
    fun findAllByStatus(status: String): List<BrandEntity>
    fun findAllByIdIn(ids: List<Long>): List<BrandEntity>
}
