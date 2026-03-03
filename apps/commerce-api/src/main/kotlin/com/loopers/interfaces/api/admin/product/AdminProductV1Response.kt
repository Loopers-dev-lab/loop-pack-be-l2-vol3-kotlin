package com.loopers.interfaces.api.admin.product

import com.loopers.application.admin.product.AdminProductResult
import java.math.BigDecimal

class AdminProductV1Response {
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
            fun from(result: AdminProductResult.Register): Register = Register(
                id = result.id,
                name = result.name,
                regularPrice = result.regularPrice,
                sellingPrice = result.sellingPrice,
                brandId = result.brandId,
                status = result.status,
                stockQuantity = result.stockQuantity,
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
            fun from(result: AdminProductResult.Update): Update = Update(
                id = result.id,
                name = result.name,
                regularPrice = result.regularPrice,
                sellingPrice = result.sellingPrice,
                imageUrl = result.imageUrl,
                thumbnailUrl = result.thumbnailUrl,
                status = result.status,
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
            fun from(result: AdminProductResult.Detail): Detail = Detail(
                id = result.id,
                name = result.name,
                regularPrice = result.regularPrice,
                sellingPrice = result.sellingPrice,
                brandId = result.brandId,
                brandName = result.brandName,
                imageUrl = result.imageUrl,
                thumbnailUrl = result.thumbnailUrl,
                likeCount = result.likeCount,
                status = result.status,
                stockQuantity = result.stockQuantity,
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
            fun from(result: AdminProductResult.Summary): Summary = Summary(
                id = result.id,
                name = result.name,
                sellingPrice = result.sellingPrice,
                brandId = result.brandId,
                status = result.status,
            )
        }
    }
}
