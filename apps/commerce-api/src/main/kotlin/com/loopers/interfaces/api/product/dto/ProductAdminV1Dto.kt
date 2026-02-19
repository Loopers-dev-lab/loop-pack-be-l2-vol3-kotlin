package com.loopers.interfaces.api.product.dto

import com.loopers.domain.catalog.CatalogCommand
import com.loopers.domain.catalog.product.entity.Product
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
    ) {
        fun toCommand(): CatalogCommand.CreateProduct {
            return CatalogCommand.CreateProduct(
                brandId = brandId,
                name = name,
                price = price,
                stock = stock,
            )
        }
    }

    data class UpdateProductRequest(
        val name: String?,
        @field:DecimalMin(value = "0", message = "가격은 0 이상이어야 합니다.")
        val price: BigDecimal?,
        @field:Min(value = 0, message = "재고는 0 이상이어야 합니다.")
        val stock: Int?,
        val status: Product.ProductStatus?,
    ) {
        fun toCommand(): CatalogCommand.UpdateProduct {
            return CatalogCommand.UpdateProduct(
                name = name,
                price = price,
                stock = stock,
                status = status,
            )
        }
    }

    data class AdminProductResponse(
        val id: Long,
        val refBrandId: Long,
        val name: String,
        val price: BigDecimal,
        val stock: Int,
        val status: Product.ProductStatus,
        val likeCount: Int,
        val createdAt: ZonedDateTime,
        val updatedAt: ZonedDateTime,
        val deletedAt: ZonedDateTime?,
    ) {
        companion object {
            fun from(product: Product): AdminProductResponse {
                return AdminProductResponse(
                    id = product.id,
                    refBrandId = product.refBrandId,
                    name = product.name,
                    price = product.price,
                    stock = product.stock,
                    status = product.status,
                    likeCount = product.likeCount,
                    createdAt = product.createdAt,
                    updatedAt = product.updatedAt,
                    deletedAt = product.deletedAt,
                )
            }
        }
    }
}
