package com.loopers.infrastructure.product

import com.loopers.domain.product.Name
import com.loopers.domain.product.ProductModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductJpaRepository : JpaRepository<ProductModel, Long> {
    fun findAllByBrandId(brandId: Long, pageable: Pageable): Page<ProductModel>
    fun findAllByBrandId(brandId: Long): List<ProductModel>
    fun existsByBrandIdAndName(brandId: Long, name: Name): Boolean

    @Modifying(clearAutomatically = true)
    @Query(
        value = "UPDATE tb_product SET like_count = like_count + 1 WHERE id = :id",
        nativeQuery = true,
    )
    fun incrementLikeCount(@Param("id") id: Long)

    @Modifying(clearAutomatically = true)
    @Query(
        value = "UPDATE tb_product SET like_count = GREATEST(like_count - 1, 0) WHERE id = :id",
        nativeQuery = true,
    )
    fun decrementLikeCount(@Param("id") id: Long)
}
