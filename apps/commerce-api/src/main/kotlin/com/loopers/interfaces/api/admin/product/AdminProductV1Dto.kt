package com.loopers.interfaces.api.admin.product

import com.loopers.application.product.ProductInfo
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.time.ZonedDateTime

class AdminProductV1Dto {
    data class CreateRequest(
        val brandId: Long,
        @field:NotBlank(message = "상품명은 필수입니다.")
        val name: String,
        @field:NotBlank(message = "상품 설명은 필수입니다.")
        val description: String,
        @field:Min(value = 0, message = "가격은 0 이상이어야 합니다.")
        val price: Long,
        @field:Min(value = 0, message = "재고 수량은 0 이상이어야 합니다.")
        val stockQuantity: Int,
        @field:NotBlank(message = "이미지 URL은 필수입니다.")
        val imageUrl: String,
    )

    data class UpdateRequest(
        @field:NotBlank(message = "상품명은 필수입니다.")
        val name: String,
        @field:NotBlank(message = "상품 설명은 필수입니다.")
        val description: String,
        @field:Min(value = 0, message = "가격은 0 이상이어야 합니다.")
        val price: Long,
        @field:Min(value = 0, message = "재고 수량은 0 이상이어야 합니다.")
        val stockQuantity: Int,
        @field:NotBlank(message = "이미지 URL은 필수입니다.")
        val imageUrl: String,
    )

    data class ProductResponse(
        val id: Long,
        val brandId: Long,
        val brandName: String?,
        val name: String,
        val description: String,
        val price: Long,
        val imageUrl: String,
        val likeCount: Int,
        val stockQuantity: Int,
        val status: String,
        val createdAt: ZonedDateTime?,
        val updatedAt: ZonedDateTime?,
    ) {
        companion object {
            fun from(info: ProductInfo): ProductResponse {
                return ProductResponse(
                    id = info.id,
                    brandId = info.brandId,
                    brandName = info.brandName,
                    name = info.name,
                    description = info.description,
                    price = info.price,
                    imageUrl = info.imageUrl,
                    likeCount = info.likeCount,
                    stockQuantity = info.stockQuantity,
                    status = info.status,
                    createdAt = info.createdAt,
                    updatedAt = info.updatedAt,
                )
            }
        }
    }
}
