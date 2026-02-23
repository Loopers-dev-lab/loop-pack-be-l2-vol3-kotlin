package com.loopers.infrastructure.catalog.product

import com.loopers.domain.BaseEntity
import com.loopers.domain.catalog.product.entity.Product
import com.loopers.domain.common.Money
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "products")
class ProductEntity(
    @Column(name = "ref_brand_id", nullable = false)
    var refBrandId: Long,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    var price: BigDecimal,

    @Column(name = "stock", nullable = false)
    var stock: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: Product.ProductStatus,

    @Column(name = "like_count", nullable = false)
    var likeCount: Int = 0,
) : BaseEntity() {

    companion object {
        fun fromDomain(product: Product): ProductEntity {
            return ProductEntity(
                refBrandId = product.refBrandId,
                name = product.name,
                price = product.price.value,
                stock = product.stock,
                status = product.status,
                likeCount = product.likeCount,
            ).also { entity ->
                if (product.id != 0L) {
                    setBaseEntityField(entity, "id", product.id)
                    setBaseEntityField(entity, "createdAt", product.createdAt)
                    setBaseEntityField(entity, "updatedAt", product.updatedAt)
                }
                product.deletedAt?.let { setBaseEntityField(entity, "deletedAt", it) }
            }
        }

        private fun setBaseEntityField(entity: BaseEntity, fieldName: String, value: Any) {
            BaseEntity::class.java.getDeclaredField(fieldName).apply {
                isAccessible = true
                set(entity, value)
            }
        }
    }

    fun toDomain(): Product = Product(
        id = id,
        refBrandId = refBrandId,
        name = name,
        price = Money(price),
        stock = stock,
        status = status,
        likeCount = likeCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}
