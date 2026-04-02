package com.loopers.infrastructure.productlike

import com.loopers.domain.productlike.ProductLike
import com.loopers.domain.productlike.ProductLikeCountDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ProductLikeJpaRepository : JpaRepository<ProductLike, Long> {
    @Query("SELECT pl FROM ProductLike pl WHERE pl.user.id = :userId AND pl.product.id = :productId")
    fun findByUserIdAndProductId(userId: Long, productId: Long): ProductLike?

    @Modifying
    @Query("DELETE FROM ProductLike pl WHERE pl.user.id = :userId AND pl.product.id = :productId")
    fun deleteByUserIdAndProductId(userId: Long, productId: Long): Int

    @Query(
        "SELECT new com.loopers.domain.productlike.ProductLikeCountDto(pl.product.id, COUNT(pl)) " +
            "FROM ProductLike pl GROUP BY pl.product.id",
    )
    fun countByProductId(): List<ProductLikeCountDto>
}
