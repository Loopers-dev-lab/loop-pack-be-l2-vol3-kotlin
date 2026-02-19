package com.loopers.interfaces.admin.product

import com.loopers.domain.product.ProductStatus
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

class AdminProductV1Dto {
    data class CreateProductRequest(
        @field:Positive(message = "브랜드 ID는 양수여야 합니다.")
        val brandId: Long,

        @field:NotBlank(message = "상품명은 비어있을 수 없습니다.")
        val name: String,

        @field:Positive(message = "가격은 0보다 커야 합니다.")
        val price: BigDecimal,

        @field:Min(value = 0, message = "재고는 0 이상이어야 합니다.")
        val stock: Int,

        val status: ProductStatus,
    )

    data class UpdateProductRequest(
        @field:NotBlank(message = "상품명은 비어있을 수 없습니다.")
        val name: String,

        @field:Positive(message = "가격은 0보다 커야 합니다.")
        val price: BigDecimal,

        @field:Min(value = 0, message = "재고는 0 이상이어야 합니다.")
        val stock: Int,

        val status: ProductStatus,
    )
}
