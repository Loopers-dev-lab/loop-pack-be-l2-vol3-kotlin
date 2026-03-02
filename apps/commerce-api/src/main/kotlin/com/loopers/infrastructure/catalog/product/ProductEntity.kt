package com.loopers.infrastructure.catalog.product

import com.loopers.domain.BaseEntity
import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.catalog.product.vo.Stock
import com.loopers.domain.common.vo.BrandId
import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.withBaseFields
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
                refBrandId = product.refBrandId.value,
                name = product.name,
                price = product.price.value,
                stock = product.stock.value,
                status = product.status,
                likeCount = product.likeCount,
            ).withBaseFields(
                id = product.id.value,
                deletedAt = product.deletedAt,
            )
        }
    }

    fun toDomain(): Product = Product(
        id = ProductId(id),
        refBrandId = BrandId(refBrandId),
        name = name,
        price = Money(price),
        stock = Stock(stock),
        status = status,
        likeCount = likeCount,
        deletedAt = deletedAt,
    )
}
