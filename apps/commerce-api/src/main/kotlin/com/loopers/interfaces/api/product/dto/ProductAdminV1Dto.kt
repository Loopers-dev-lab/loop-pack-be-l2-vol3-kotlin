package com.loopers.interfaces.api.product.dto

import com.loopers.application.catalog.product.ProductInfo
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.ZonedDateTime

class ProductAdminV1Dto {
    data class CreateProductRequest(
        val brandId: Long,
        @field:NotBlank(message = "상품명은 필수입니다.")
        val name: String,
        @field:DecimalMin(value = "0", message = "가격은 0 이상이어야 합니다.")
        val price: BigDecimal,
        @field:Min(value = 0, message = "재고는 0 이상이어야 합니다.")
        val stock: Int,
    )

    data class UpdateProductRequest(
        val name: String?,
        @field:DecimalMin(value = "0", message = "가격은 0 이상이어야 합니다.")
        val price: BigDecimal?,
        @field:Min(value = 0, message = "재고는 0 이상이어야 합니다.")
        val stock: Int?,
        val status: String?,
    )

    data class AdminProductResponse(
        val id: Long,
        val refBrandId: Long,
        val name: String,
        val price: BigDecimal,
        val stock: Int,
        val status: ProductStatusDto,
        val likeCount: Int,
        val deletedAt: ZonedDateTime?,
    ) {
        companion object {
            fun from(info: ProductInfo): AdminProductResponse {
                return AdminProductResponse(
                    id = info.id,
                    refBrandId = info.brandId,
                    name = info.name,
                    price = info.price,
                    stock = info.stock,
                    status = ProductStatusDto.valueOf(info.status),
                    likeCount = info.likeCount,
                    deletedAt = info.deletedAt,
                )
            }
        }
    }

    enum class ProductStatusDto { ON_SALE, SOLD_OUT, HIDDEN }
}
