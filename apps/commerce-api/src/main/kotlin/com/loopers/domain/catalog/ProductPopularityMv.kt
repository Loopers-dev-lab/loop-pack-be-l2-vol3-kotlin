package com.loopers.domain.catalog

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable

@Entity
@Immutable
@Table(
    name = "product_popularity_mv",
    indexes = [
        Index(name = "idx_mv_rank", columnList = "popularity_rank ASC"),
        Index(name = "idx_mv_brand_rank", columnList = "brand_id, popularity_rank ASC"),
    ],
)
class ProductPopularityMv(
    @Id
    @Column(name = "product_id")
    val productId: Long = 0,

    @Column(name = "brand_id", nullable = false)
    val brandId: Long = 0,

    @Column(name = "like_count", nullable = false)
    val likeCount: Int = 0,

    @Column(name = "popularity_rank", nullable = false)
    val popularityRank: Long = 0,
)
