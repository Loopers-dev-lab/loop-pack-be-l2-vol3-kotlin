package com.loopers.infrastructure.product

import com.loopers.domain.product.ProductModel
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductJpaRepository : JpaRepository<ProductModel, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): ProductModel?
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<ProductModel>
    fun findAllByBrandIdAndDeletedAtIsNull(brandId: Long, pageable: Pageable): Page<ProductModel>
    fun findAllByIdInAndDeletedAtIsNull(ids: List<Long>): List<ProductModel>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductModel p WHERE p.id IN :ids AND p.deletedAt IS NULL")
    fun findAllByIdsForUpdate(@Param("ids") ids: List<Long>): List<ProductModel>
}
