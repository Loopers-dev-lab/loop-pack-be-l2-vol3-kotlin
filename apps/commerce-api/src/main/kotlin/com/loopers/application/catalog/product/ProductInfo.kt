package com.loopers.application.catalog.product

import com.loopers.domain.catalog.product.entity.Product
import java.math.BigDecimal
import java.time.ZonedDateTime

data class ProductInfo(
    val id: Long,
    val brandId: Long,
    val name: String,
    val price: BigDecimal,
    val stock: Int,
    val likeCount: Int,
    val status: String,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
    val deletedAt: ZonedDateTime?,
) {
    companion object {
        fun from(product: Product): ProductInfo = ProductInfo(
            id = product.id,
            brandId = product.refBrandId,
            name = product.name,
            price = product.price.value,
            stock = product.stock,
            likeCount = product.likeCount,
            status = product.status.name,
            createdAt = product.createdAt,
            updatedAt = product.updatedAt,
            deletedAt = product.deletedAt,
        )
    }
}
