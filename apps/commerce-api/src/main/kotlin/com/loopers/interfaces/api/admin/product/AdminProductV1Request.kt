package com.loopers.interfaces.api.admin.product

import com.loopers.application.product.ProductCommand
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class AdminProductRegisterRequest(
    @field:NotNull(message = "브랜드 ID는 필수입니다.")
    val brandId: Long,
    @field:NotBlank(message = "상품명은 필수입니다.")
    @field:Size(min = 1, max = 100, message = "상품명은 1~100자여야 합니다.")
    val name: String,
    @field:NotBlank(message = "상품 설명은 필수입니다.")
    val description: String,
    @field:NotNull(message = "가격은 필수입니다.")
    @field:Min(value = 0, message = "가격은 0 이상이어야 합니다.")
    val price: Long,
    @field:NotNull(message = "재고는 필수입니다.")
    @field:Min(value = 0, message = "재고는 0 이상이어야 합니다.")
    val stock: Int,
    @field:NotBlank(message = "이미지 URL은 필수입니다.")
    val imageUrl: String,
) {
    fun toCommand(): ProductCommand.Register {
        return ProductCommand.Register(
            brandId = brandId,
            name = name.trim(),
            description = description,
            price = price,
            stock = stock,
            imageUrl = imageUrl,
        )
    }
}

data class AdminProductUpdateRequest(
    @field:NotBlank(message = "상품명은 필수입니다.")
    @field:Size(min = 1, max = 100, message = "상품명은 1~100자여야 합니다.")
    val name: String,
    @field:NotBlank(message = "상품 설명은 필수입니다.")
    val description: String,
    @field:NotNull(message = "가격은 필수입니다.")
    @field:Min(value = 0, message = "가격은 0 이상이어야 합니다.")
    val price: Long,
    @field:NotNull(message = "재고는 필수입니다.")
    @field:Min(value = 0, message = "재고는 0 이상이어야 합니다.")
    val stock: Int,
    @field:NotBlank(message = "이미지 URL은 필수입니다.")
    val imageUrl: String,
) {
    fun toCommand(productId: Long): ProductCommand.Update {
        return ProductCommand.Update(
            productId = productId,
            name = name.trim(),
            description = description,
            price = price,
            stock = stock,
            imageUrl = imageUrl,
        )
    }
}
