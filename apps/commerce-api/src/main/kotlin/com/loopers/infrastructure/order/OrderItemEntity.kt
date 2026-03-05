package com.loopers.infrastructure.order

import com.loopers.infrastructure.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal

@Table(name = "order_item")
@Entity
class OrderItemEntity(
    id: Long? = null,
    @Column(name = "product_id", nullable = false)
    val productId: Long,
    @Column(name = "product_name", nullable = false)
    val productName: String,
    @Column(name = "brand_id", nullable = false)
    val brandId: Long,
    @Column(name = "brand_name", nullable = false)
    val brandName: String,
    @Column(name = "regular_price", nullable = false)
    val regularPrice: BigDecimal,
    @Column(name = "selling_price", nullable = false)
    val sellingPrice: BigDecimal,
    @Column(name = "thumbnail_url")
    val thumbnailUrl: String?,
    @Column(nullable = false)
    val quantity: Int,
) : BaseEntity() {

    init {
        this.id = id
    }
}
