package com.loopers.application.admin.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductStock
import java.math.BigDecimal

class AdminProductResult {
    data class Register(
        val id: Long,
        val name: String,
        val regularPrice: BigDecimal,
        val sellingPrice: BigDecimal,
        val brandId: Long,
        val status: String,
        val stockQuantity: Int,
    ) {
        companion object {
            fun from(product: Product, stock: ProductStock): Register = Register(
                id = product.id!!,
                name = product.name,
                regularPrice = product.regularPrice.amount,
                sellingPrice = product.sellingPrice.amount,
                brandId = product.brandId,
                status = product.status.name,
                stockQuantity = stock.quantity.value,
            )
        }
    }

    data class Update(
        val id: Long,
        val name: String,
        val regularPrice: BigDecimal,
        val sellingPrice: BigDecimal,
        val imageUrl: String?,
        val thumbnailUrl: String?,
        val status: String,
    ) {
        companion object {
            fun from(product: Product): Update = Update(
                id = product.id!!,
                name = product.name,
                regularPrice = product.regularPrice.amount,
                sellingPrice = product.sellingPrice.amount,
                imageUrl = product.imageUrl,
                thumbnailUrl = product.thumbnailUrl,
                status = product.status.name,
            )
        }
    }

    data class Detail(
        val id: Long,
        val name: String,
        val regularPrice: BigDecimal,
        val sellingPrice: BigDecimal,
        val brandId: Long,
        val brandName: String,
        val imageUrl: String?,
        val thumbnailUrl: String?,
        val likeCount: Int,
        val status: String,
        val stockQuantity: Int,
    ) {
        companion object {
            fun from(product: Product, brand: Brand, stock: ProductStock): Detail = Detail(
                id = product.id!!,
                name = product.name,
                regularPrice = product.regularPrice.amount,
                sellingPrice = product.sellingPrice.amount,
                brandId = product.brandId,
                brandName = brand.name.value,
                imageUrl = product.imageUrl,
                thumbnailUrl = product.thumbnailUrl,
                likeCount = product.likeCount,
                status = product.status.name,
                stockQuantity = stock.quantity.value,
            )
        }
    }

    data class Summary(
        val id: Long,
        val name: String,
        val sellingPrice: BigDecimal,
        val brandId: Long,
        val status: String,
    ) {
        companion object {
            fun from(product: Product): Summary = Summary(
                id = product.id!!,
                name = product.name,
                sellingPrice = product.sellingPrice.amount,
                brandId = product.brandId,
                status = product.status.name,
            )
        }
    }
}
