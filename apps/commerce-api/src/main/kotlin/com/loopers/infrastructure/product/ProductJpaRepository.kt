package com.loopers.infrastructure.product

import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface ProductJpaRepository : JpaRepository<ProductModel, Long> {
    fun findAllByBrandId(brandId: Long, pageable: Pageable): Page<ProductModel>

    fun findAllByIdIn(ids: List<Long>): List<ProductModel>

    fun findAllByBrandIdAndStatus(brandId: Long, status: ProductStatus): List<ProductModel>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductModel p WHERE p.id = :id")
    fun findByIdWithLock(id: Long): ProductModel?
}
