package com.loopers.interfaces.api.product

import com.loopers.application.product.ProductInfo
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

class ProductV1Dto {

    @Schema(description = "상품 응답 (대고객)")
    data class ProductResponse(
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
    ) {
        companion object {
            fun from(info: ProductInfo): ProductResponse {
                return ProductResponse(
                    id = info.id,
                    brandId = info.brandId,
                    name = info.name,
                    price = info.price,
                    stock = info.stock,
                    description = info.description,
                    imageUrl = info.imageUrl,
                )
            }
        }
    }
}
