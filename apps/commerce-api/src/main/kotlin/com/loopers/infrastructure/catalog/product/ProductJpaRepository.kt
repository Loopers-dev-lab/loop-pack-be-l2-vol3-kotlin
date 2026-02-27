package com.loopers.infrastructure.catalog.product

import com.loopers.domain.catalog.product.ProductStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ProductJpaRepository : JpaRepository<ProductEntity, Long> {

    @Query("SELECT p FROM ProductEntity p WHERE p.deletedAt IS NULL AND p.status = :status AND (:brandId IS NULL OR p.brandId = :brandId) ORDER BY p.createdAt DESC")
    fun findAllByStatusOrderByCreatedAtDesc(brandId: Long?, status: ProductStatus, pageable: Pageable): List<ProductEntity>

    @Query("SELECT p FROM ProductEntity p WHERE p.deletedAt IS NULL AND p.status = :status AND (:brandId IS NULL OR p.brandId = :brandId) ORDER BY p.price ASC")
    fun findAllByStatusOrderByPriceAsc(brandId: Long?, status: ProductStatus, pageable: Pageable): List<ProductEntity>

    @Query("SELECT p FROM ProductEntity p WHERE p.deletedAt IS NULL AND p.status = :status AND (:brandId IS NULL OR p.brandId = :brandId) ORDER BY p.likeCount DESC")
    fun findAllByStatusOrderByLikeCountDesc(brandId: Long?, status: ProductStatus, pageable: Pageable): List<ProductEntity>

    @Query("SELECT p FROM ProductEntity p WHERE p.deletedAt IS NULL AND p.brandId = :brandId")
    fun findAllByBrandId(brandId: Long): List<ProductEntity>
}
