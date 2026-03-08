package com.loopers.infrastructure.catalog

import com.loopers.domain.catalog.ProductModel
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.ZonedDateTime

interface ProductJpaRepository : JpaRepository<ProductModel, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): ProductModel?
    fun findAllByDeletedAtIsNull(pageable: Pageable): Slice<ProductModel>
    fun findAllByBrandIdAndDeletedAtIsNull(brandId: Long): List<ProductModel>
    fun findAllByBrandIdAndDeletedAtIsNull(brandId: Long, pageable: Pageable): Slice<ProductModel>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductModel p WHERE p.id = :id AND p.deletedAt IS NULL")
    fun findByIdWithLock(@Param("id") id: Long): ProductModel?

    @Modifying
    @Query(
        "UPDATE ProductModel p SET p.likeCount = p.likeCount + 1, p.updatedAt = :now " +
            "WHERE p.id = :id AND p.deletedAt IS NULL",
    )
    fun increaseLikeCount(
        @Param("id") id: Long,
        @Param("now") now: ZonedDateTime,
    ): Int

    @Modifying
    @Query(
        "UPDATE ProductModel p SET p.likeCount = p.likeCount - 1, p.updatedAt = :now " +
            "WHERE p.id = :id AND p.likeCount > 0 AND p.deletedAt IS NULL",
    )
    fun decreaseLikeCount(
        @Param("id") id: Long,
        @Param("now") now: ZonedDateTime,
    ): Int
}
