package com.loopers.interfaces.api.product

import com.loopers.application.product.ProductUseCase
import com.loopers.application.product.ProductInfo
import jakarta.validation.constraints.NotBlank

class ProductV1Dto {

    data class RegisterRequest(
        val brandId: Long,
        @field:NotBlank val name: String,
        val price: Long,
        val description: String,
        val stock: Int,
    ) {
        fun toCommand() = ProductUseCase.RegisterCommand(
            brandId = brandId,
            name = name,
            price = price,
            description = description,
            stock = stock,
        )
    }

    data class ChangeInfoRequest(
        @field:NotBlank val name: String,
        val price: Long,
        val description: String,
    ) {
        fun toCommand() = ProductUseCase.ChangeInfoCommand(
            name = name,
            price = price,
            description = description,
        )
    }

    data class DetailResponse(
        val id: Long,
        val brandId: Long,
        val brandName: String,
        val name: String,
        val price: Long,
        val description: String,
        val stock: Int,
        val status: String,
        val likeCount: Long,
    ) {
        companion object {
            fun from(info: ProductInfo.Detail) = DetailResponse(
                id = info.id,
                brandId = info.brandId,
                brandName = info.brandName,
                name = info.name,
                price = info.price,
                description = info.description,
                stock = info.stock,
                status = info.status,
                likeCount = info.likeCount,
            )
        }
    }

    data class MainResponse(
        val id: Long,
        val brandId: Long,
        val brandName: String,
        val name: String,
        val price: Long,
        val stock: Int,
        val status: String,
        val likeCount: Long,
    ) {
        companion object {
            fun from(info: ProductInfo.Main) = MainResponse(
                id = info.id,
                brandId = info.brandId,
                brandName = info.brandName,
                name = info.name,
                price = info.price,
                stock = info.stock,
                status = info.status,
                likeCount = info.likeCount,
            )
        }
    }
}
