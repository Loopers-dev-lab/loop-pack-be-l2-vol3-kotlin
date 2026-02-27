package com.loopers.domain.order

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import org.hibernate.annotations.Comment

@Embeddable
data class ProductSnapshot(
    @Comment("주문 당시 상품명")
    @Column(name = "snapshot_product_name", nullable = false)
    val productName: String,

    @Comment("주문 당시 브랜드명")
    @Column(name = "snapshot_brand_name", nullable = false)
    val brandName: String,

    @Comment("브랜드 원본 참조")
    @Column(name = "snapshot_brand_id", nullable = false)
    val brandId: Long,

    @Comment("주문 당시 상품 이미지")
    @Column(name = "snapshot_image_url")
    val imageUrl: String?,
) {
    companion object {
        fun from(product: Product, brand: Brand): ProductSnapshot {
            return ProductSnapshot(
                productName = product.name,
                brandName = brand.name,
                brandId = brand.id,
                imageUrl = product.imageUrl,
            )
        }
    }
}
