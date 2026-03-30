package com.loopers.domain.product

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

/**
 * commerce-streamer에서 products 테이블에 재고 차감 네이티브 쿼리를 수행하기 위한 최소 엔티티.
 * DDL 생성과 테이블 매핑 목적으로만 사용한다.
 */
@Entity
@Table(name = "products")
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
