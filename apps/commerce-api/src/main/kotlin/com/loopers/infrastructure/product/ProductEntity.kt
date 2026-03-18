package com.loopers.infrastructure.product

import com.loopers.domain.product.Product
import com.loopers.infrastructure.AdminAuditEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal

@Table(
    name = "product",
    indexes = [
        Index(
            name = "idx_product_brand_status_deleted_like_id",
            columnList = "brand_id, status, deleted_at, like_count DESC, id DESC",
        ),
        Index(
            name = "idx_product_brand_status_deleted_price_id",
            columnList = "brand_id, status, deleted_at, selling_price ASC, id DESC",
        ),
    ],
)
@Entity
class ProductEntity(
    id: Long? = null,
    @Column(nullable = false)
    var name: String,
    @Column(name = "regular_price", nullable = false)
    var regularPrice: BigDecimal,
    @Column(name = "selling_price", nullable = false)
    var sellingPrice: BigDecimal,
    @Column(name = "brand_id", nullable = false)
    val brandId: Long,
    @Column(name = "image_url")
    var imageUrl: String? = null,
    @Column(name = "thumbnail_url")
    var thumbnailUrl: String? = null,
    @Column(name = "like_count", nullable = false)
    var likeCount: Int = 0,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: Product.Status,
    createdBy: String,
    updatedBy: String,
) : AdminAuditEntity() {

    init {
        this.id = id
        this.createdBy = createdBy
        this.updatedBy = updatedBy
    }
}
