package com.loopers.infrastructure.catalog.product

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ProductJpaRepository : JpaRepository<ProductEntity, Long> {

    @Query("SELECT p FROM ProductEntity p WHERE p.deletedAt IS NULL AND (:brandId IS NULL OR p.brandId = :brandId) ORDER BY p.createdAt DESC")
    fun findAllActiveOrderByCreatedAtDesc(brandId: Long?, pageable: Pageable): List<ProductEntity>

    @Query("SELECT p FROM ProductEntity p WHERE p.deletedAt IS NULL AND (:brandId IS NULL OR p.brandId = :brandId) ORDER BY p.price ASC")
    fun findAllActiveOrderByPriceAsc(brandId: Long?, pageable: Pageable): List<ProductEntity>

    @Query("SELECT p FROM ProductEntity p WHERE p.deletedAt IS NULL AND (:brandId IS NULL OR p.brandId = :brandId) ORDER BY p.likeCount DESC")
    fun findAllActiveOrderByLikeCountDesc(brandId: Long?, pageable: Pageable): List<ProductEntity>

    @Query("SELECT p FROM ProductEntity p WHERE p.deletedAt IS NULL AND p.brandId = :brandId")
    fun findAllActiveByBrandId(brandId: Long): List<ProductEntity>
}
