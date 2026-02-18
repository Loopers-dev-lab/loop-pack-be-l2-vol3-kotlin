package com.loopers.infrastructure.product

import com.loopers.domain.BaseEntity
import com.loopers.domain.product.ProductStatus
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "product")
class ProductEntity(
    id: Long?,

    @Column(name = "brand_id", nullable = false)
    val brandId: Long,

    @Column(name = "name", nullable = false, length = 200)
    val name: String,

    @Column(name = "description", columnDefinition = "TEXT")
    val description: String?,

    @Column(name = "price", nullable = false)
    val price: Long,

    @Column(name = "stock", nullable = false)
    val stock: Int,

    @Column(name = "thumbnail_url", length = 500)
    val thumbnailUrl: String?,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: ProductStatus,

    @Column(name = "like_count", nullable = false)
    val likeCount: Int,

    deletedAt: ZonedDateTime?,

    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true)
    val images: MutableList<ProductImageEntity> = mutableListOf(),
) : BaseEntity() {
    init {
        this.id = id
        if (deletedAt != null) {
            this.deletedAt = deletedAt
        }
    }
}
