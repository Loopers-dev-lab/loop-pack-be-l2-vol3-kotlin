package com.loopers.infrastructure.product

import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductJpaRepository : JpaRepository<Product, Long> {

    fun findAllByIdInAndDeletedAtIsNull(ids: List<Long>): List<Product>

    fun findByBrandIdAndDeletedAtIsNull(brandId: Long): List<Product>

    // 대고객: ACTIVE + displayYn=true + deletedAt IS NULL
    fun findAllByStatusAndDisplayYnAndDeletedAtIsNull(
        status: ProductStatus,
        displayYn: Boolean,
        pageable: Pageable,
    ): Page<Product>

    // 대고객: brandId 필터 추가
    fun findAllByBrandIdAndStatusAndDisplayYnAndDeletedAtIsNull(
        brandId: Long,
        status: ProductStatus,
        displayYn: Boolean,
        pageable: Pageable,
    ): Page<Product>

    // 어드민: deletedAt IS NULL
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<Product>

    // 어드민: brandId 필터 추가
    fun findAllByBrandIdAndDeletedAtIsNull(brandId: Long, pageable: Pageable): Page<Product>

    @Modifying
    @Query(
        "UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity " +
            "WHERE p.id = :productId AND p.stockQuantity >= :quantity AND p.deletedAt IS NULL",
    )
    fun decreaseStock(
        @Param("productId") productId: Long,
        @Param("quantity") quantity: Int,
    ): Int
}
