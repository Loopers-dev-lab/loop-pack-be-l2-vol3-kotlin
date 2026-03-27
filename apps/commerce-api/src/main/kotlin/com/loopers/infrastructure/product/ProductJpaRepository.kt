package com.loopers.infrastructure.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

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

    @Query(
        """
        SELECT p.id
          FROM ProductEntity p
         WHERE p.brandId = :brandId
           AND p.deletedAt IS NULL
        """,
    )
    fun findIdsByBrandIdAndDeletedAtIsNull(@Param("brandId") brandId: Long): List<Long>
    fun findAllByBrandIdAndDeletedAtIsNull(brandId: Long): List<ProductEntity>
    fun findAllByIdInAndDeletedAtIsNull(ids: List<Long>): List<ProductEntity>

    @Query(
        value = """
        SELECT p
          FROM ProductEntity p
         WHERE p.status = :productStatus
           AND p.deletedAt IS NULL
           AND p.brandId IN (
               SELECT b.id
                 FROM BrandEntity b
                WHERE b.status = :brandStatus
                  AND b.deletedAt IS NULL
                  AND (:brandId IS NULL OR b.id = :brandId)
           )
        """,
        countQuery = """
        SELECT COUNT(p)
          FROM ProductEntity p
         WHERE p.status = :productStatus
           AND p.deletedAt IS NULL
           AND p.brandId IN (
               SELECT b.id
                 FROM BrandEntity b
                WHERE b.status = :brandStatus
                  AND b.deletedAt IS NULL
                  AND (:brandId IS NULL OR b.id = :brandId)
           )
        """,
    )
    fun findAllByStatusAndActiveBrand(
        @Param("productStatus") productStatus: Product.Status,
        @Param("brandStatus") brandStatus: Brand.Status,
        @Param("brandId") brandId: Long?,
        pageable: Pageable,
    ): Page<ProductEntity>

    @Modifying
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
