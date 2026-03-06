package com.loopers.infrastructure.product

import com.loopers.domain.product.ProductStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface ProductJpaRepository : JpaRepository<ProductJpaModel, Long> {
    fun findAllByBrandId(brandId: Long, pageable: Pageable): Page<ProductJpaModel>

    fun findAllByIdIn(ids: List<Long>): List<ProductJpaModel>

    fun findAllByBrandIdAndStatus(brandId: Long, status: ProductStatus): List<ProductJpaModel>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductJpaModel p WHERE p.id = :id")
    fun findByIdWithLock(id: Long): ProductJpaModel?
}
