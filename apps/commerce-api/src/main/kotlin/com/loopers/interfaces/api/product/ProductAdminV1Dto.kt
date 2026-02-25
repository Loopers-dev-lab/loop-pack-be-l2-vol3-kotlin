package com.loopers.interfaces.api.product

import com.loopers.domain.product.CreateProductCommand
import com.loopers.domain.product.Product
import com.loopers.domain.product.UpdateProductCommand
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.ZonedDateTime

class ProductAdminV1Dto {

    @Schema(description = "상품 응답 (어드민)")
    data class ProductAdminResponse(
        @Schema(description = "상품 ID", example = "1")
        val id: Long,
        @Schema(description = "브랜드 ID", example = "1")
        val brandId: Long,
        @Schema(description = "상품명", example = "에어맥스 90")
        val name: String,
        @Schema(description = "가격", example = "129000")
        val price: BigDecimal,
        @Schema(description = "재고", example = "100")
        val stock: Int,
        @Schema(description = "상품 설명", example = "나이키 에어맥스 90")
        val description: String?,
        @Schema(description = "이미지 URL", example = "https://example.com/airmax90.jpg")
        val imageUrl: String?,
        @Schema(description = "생성일시")
        val createdAt: ZonedDateTime,
        @Schema(description = "수정일시")
        val updatedAt: ZonedDateTime,
    ) {
        companion object {
            fun from(product: Product): ProductAdminResponse {
                return ProductAdminResponse(
                    id = product.id,
                    brandId = product.brandId,
                    name = product.name,
                    price = product.price,
                    stock = product.stock,
                    description = product.description,
                    imageUrl = product.imageUrl,
                    createdAt = product.createdAt,
                    updatedAt = product.updatedAt,
                )
            }
        }
    }

    @Schema(description = "상품 등록 요청")
    data class CreateRequest(
        @Schema(description = "브랜드 ID", example = "1")
        val brandId: Long,
        @Schema(description = "상품명", example = "에어맥스 90")
        val name: String,
        @Schema(description = "가격", example = "129000")
        val price: BigDecimal,
        @Schema(description = "재고", example = "100")
        val stock: Int,
        @Schema(description = "상품 설명", example = "나이키 에어맥스 90")
        val description: String?,
        @Schema(description = "이미지 URL", example = "https://example.com/airmax90.jpg")
        val imageUrl: String?,
    ) {
        fun toCommand(): CreateProductCommand {
            return CreateProductCommand(
                brandId = brandId,
                name = name,
                price = price,
                stock = stock,
                description = description,
                imageUrl = imageUrl,
            )
        }
    }

    @Schema(description = "상품 수정 요청")
    data class UpdateRequest(
        @Schema(description = "상품명", example = "에어포스 1")
        val name: String,
        @Schema(description = "가격", example = "139000")
        val price: BigDecimal,
        @Schema(description = "재고", example = "50")
        val stock: Int,
        @Schema(description = "상품 설명", example = "나이키 에어포스 1")
        val description: String?,
        @Schema(description = "이미지 URL", example = "https://example.com/airforce1.jpg")
        val imageUrl: String?,
    ) {
        fun toCommand(): UpdateProductCommand {
            return UpdateProductCommand(
                name = name,
                price = price,
                stock = stock,
                description = description,
                imageUrl = imageUrl,
            )
        }
    }
}
