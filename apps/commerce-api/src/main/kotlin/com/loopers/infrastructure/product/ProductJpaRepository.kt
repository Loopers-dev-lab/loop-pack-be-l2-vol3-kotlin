package com.loopers.infrastructure.product

import com.loopers.domain.product.Product
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface ProductJpaRepository : JpaRepository<ProductEntity, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): ProductEntity?
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<ProductEntity>
    fun findAllByBrandIdAndDeletedAtIsNull(brandId: Long, pageable: Pageable): Page<ProductEntity>
    fun findAllByStatusAndDeletedAtIsNull(status: Product.Status, pageable: Pageable): Page<ProductEntity>
    fun findAllByStatusAndBrandIdAndDeletedAtIsNull(
        status: Product.Status,
        brandId: Long,
        pageable: Pageable,
    ): Page<ProductEntity>
    fun findAllByBrandIdAndDeletedAtIsNull(brandId: Long): List<ProductEntity>
    fun findAllByIdInAndDeletedAtIsNull(ids: List<Long>): List<ProductEntity>

    @Modifying
    @Transactional
    @Query(
        """
        UPDATE ProductEntity p
           SET p.likeCount = p.likeCount + 1
         WHERE p.id = :productId
           AND p.deletedAt IS NULL
           AND p.status = com.loopers.domain.product.Product.Status.ACTIVE
        """,
    )
    fun incrementLikeCount(@Param("productId") productId: Long): Int

    @Modifying
    @Transactional
    @Query(
        """
        UPDATE ProductEntity p
           SET p.likeCount = p.likeCount - 1
         WHERE p.id = :productId
           AND p.deletedAt IS NULL
           AND p.likeCount > 0
        """,
    )
    fun decrementLikeCount(@Param("productId") productId: Long): Int
}
