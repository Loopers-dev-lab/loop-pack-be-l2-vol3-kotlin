package com.loopers.infrastructure.brand

import com.loopers.domain.brand.Brand
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface BrandJpaRepository : JpaRepository<Brand, Long> {

    fun findByIdAndDeletedAtIsNull(id: Long): Brand?

    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<Brand>

    fun existsByIdAndDeletedAtIsNull(id: Long): Boolean

    fun existsByNameAndDeletedAtIsNull(name: String): Boolean
    fun existsByNameAndDeletedAtIsNullAndIdNot(name: String, id: Long): Boolean

    fun findAllByIdIn(ids: List<Long>): List<Brand>
}
