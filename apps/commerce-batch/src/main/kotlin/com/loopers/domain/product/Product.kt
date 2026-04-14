package com.loopers.domain.product

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * commerce-batch에서 products 테이블 DDL 생성 및 네이티브 쿼리 실행을 위한 최소 엔티티.
 */
@Entity
@Table(
    name = "products",
    indexes = [
        Index(name = "idx_products_brand_id_created_at", columnList = "brand_id, created_at"),
        Index(name = "idx_products_brand_id_likes", columnList = "brand_id, likes"),
        Index(name = "idx_products_brand_id_price", columnList = "brand_id, price"),
        Index(name = "idx_products_deleted_at_created_at", columnList = "deleted_at, created_at"),
        Index(name = "idx_products_deleted_at_likes", columnList = "deleted_at, likes"),
        Index(name = "idx_products_deleted_at_price", columnList = "deleted_at, price"),
    ],
)
class Product(
    @Column(nullable = false)
    var name: String = "",

    var description: String? = null,

    @Column(nullable = false)
    var price: Long = 0,

    @Column(nullable = false)
    var likes: Int = 0,

    @Column(name = "stock_quantity", nullable = false)
    var stockQuantity: Int = 0,

    @Column(name = "brand_id", nullable = false)
    var brandId: Long = 0,
) : BaseEntity()
