package com.loopers.infrastructure.catalog

import com.loopers.domain.catalog.ProductModel
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.ZonedDateTime

interface ProductJpaRepository : JpaRepository<ProductModel, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): ProductModel?
    fun findAllByDeletedAtIsNull(pageable: Pageable): Slice<ProductModel>
    fun findAllByBrandIdAndDeletedAtIsNull(brandId: Long): List<ProductModel>
    fun findAllByBrandIdAndDeletedAtIsNull(brandId: Long, pageable: Pageable): Slice<ProductModel>

    @Modifying
    @Query(
        "UPDATE ProductModel p SET p.quantity = p.quantity - :quantity, p.version = p.version + 1, p.updatedAt = :now " +
            "WHERE p.id = :id AND p.quantity >= :quantity AND p.deletedAt IS NULL",
    )
    fun decreaseStock(
        @Param("id") id: Long,
        @Param("quantity") quantity: Int,
        @Param("now") now: ZonedDateTime,
    ): Int

    @Modifying
    @Query(
        "UPDATE ProductModel p SET p.quantity = p.quantity + :quantity, p.version = p.version + 1, p.updatedAt = :now " +
            "WHERE p.id = :id AND p.deletedAt IS NULL",
    )
    fun increaseStock(
        @Param("id") id: Long,
        @Param("quantity") quantity: Int,
        @Param("now") now: ZonedDateTime,
    ): Int
}
