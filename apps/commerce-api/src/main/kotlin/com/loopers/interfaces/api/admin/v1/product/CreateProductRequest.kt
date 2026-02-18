package com.loopers.interfaces.api.admin.v1.product

import com.loopers.application.product.ProductImageCommand
import com.loopers.application.product.RegisterProductCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateProductRequest(
    @field:NotNull(message = "브랜드 ID는 필수입니다.")
    val brandId: Long,

    @field:NotBlank(message = "상품명은 필수입니다.")
    @field:Size(max = 200, message = "상품명은 200자 이내여야 합니다.")
    val name: String,

    val description: String?,

    @field:NotNull(message = "가격은 필수입니다.")
    @field:Min(value = 0, message = "가격은 0 이상이어야 합니다.")
    val price: Long,

    @field:NotNull(message = "재고는 필수입니다.")
    @field:Min(value = 0, message = "재고는 0 이상이어야 합니다.")
    val stock: Int,

    @field:Size(max = 500, message = "썸네일 URL은 500자 이내여야 합니다.")
    val thumbnailUrl: String?,

    @field:Valid
    val images: List<ProductImageRequest> = emptyList(),
) {
    fun toCommand() = RegisterProductCommand(
        brandId = brandId,
        name = name,
        description = description,
        price = price,
        stock = stock,
        thumbnailUrl = thumbnailUrl,
        images = images.map { ProductImageCommand(imageUrl = it.imageUrl, displayOrder = it.displayOrder) },
    )
}

data class ProductImageRequest(
    @field:NotBlank(message = "이미지 URL은 필수입니다.")
    @field:Size(max = 500, message = "이미지 URL은 500자 이내여야 합니다.")
    val imageUrl: String,

    @field:NotNull(message = "표시 순서는 필수입니다.")
    @field:Min(value = 0, message = "표시 순서는 0 이상이어야 합니다.")
    val displayOrder: Int,
)
