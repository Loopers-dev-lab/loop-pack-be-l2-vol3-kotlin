package com.loopers.interfaces.api.admin.product

import com.loopers.application.admin.product.AdminProductCommand
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import java.math.BigDecimal

class AdminProductV1Request {
    data class Register(
        @field:NotBlank
        val name: String,
        @field:NotNull
        @field:DecimalMin("0")
        val regularPrice: BigDecimal,
        @field:NotNull
        @field:DecimalMin("0")
        val sellingPrice: BigDecimal,
        @field:NotNull
        val brandId: Long,
        @field:Min(0)
        val initialStock: Int,
        val imageUrl: String? = null,
        val thumbnailUrl: String? = null,
    ) {
        fun toCommand(admin: String): AdminProductCommand.Register = AdminProductCommand.Register(
            name = name,
            regularPrice = regularPrice,
            sellingPrice = sellingPrice,
            brandId = brandId,
            initialStock = initialStock,
            imageUrl = imageUrl,
            thumbnailUrl = thumbnailUrl,
            admin = admin,
        )
    }

    data class Update(
        @field:NotBlank
        val name: String,
        @field:NotNull
        @field:DecimalMin("0")
        val regularPrice: BigDecimal,
        @field:NotNull
        @field:DecimalMin("0")
        val sellingPrice: BigDecimal,
        @field:NotBlank
        @field:Pattern(regexp = "ACTIVE|INACTIVE", message = "ACTIVE 또는 INACTIVE만 허용됩니다.")
        val status: String,
        val imageUrl: String? = null,
        val thumbnailUrl: String? = null,
    ) {
        fun toCommand(productId: Long, admin: String): AdminProductCommand.Update = AdminProductCommand.Update(
            productId = productId,
            name = name,
            regularPrice = regularPrice,
            sellingPrice = sellingPrice,
            status = status,
            imageUrl = imageUrl,
            thumbnailUrl = thumbnailUrl,
            admin = admin,
        )
    }
}
