package com.loopers.interfaces.api.product

import com.loopers.application.product.CreateProductCriteria
import com.loopers.application.product.ProductResult
import com.loopers.application.product.UpdateProductCriteria
import com.loopers.domain.Money
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.time.ZonedDateTime

class ProductAdminV1Dto {

    data class GetProductsAdminRequest(
        val page: Int = 0,
        val size: Int = 20,
        val brandId: Long? = null,
    ) {
        fun toPageable(): Pageable {
            return PageRequest.of(page, size)
        }
    }

    data class CreateRequest(
        val brandId: Long,
        val name: String,
        val description: String?,
        val price: Money,
        val stockQuantity: Int,
        val displayYn: Boolean,
        val imageUrl: String?,
    ) {
        fun toCriteria(): CreateProductCriteria {
            return CreateProductCriteria(
                brandId = brandId,
                name = name,
                description = description,
                price = price,
                stockQuantity = stockQuantity,
                displayYn = displayYn,
                imageUrl = imageUrl,
            )
        }
    }

    data class UpdateRequest(
        val name: String,
        val description: String?,
        val price: Money,
        val stockQuantity: Int,
        val status: String,
        val displayYn: Boolean,
        val imageUrl: String?,
    ) {
        fun toCriteria(): UpdateProductCriteria {
            return UpdateProductCriteria(
                name = name,
                description = description,
                price = price,
                stockQuantity = stockQuantity,
                status = status,
                displayYn = displayYn,
                imageUrl = imageUrl,
            )
        }
    }

    data class ProductAdminResponse(
        val id: Long,
        val brandId: Long,
        val brandName: String,
        val name: String,
        val description: String?,
        val price: Money,
        val stockQuantity: Int,
        val likeCount: Int,
        val status: String,
        val displayYn: Boolean,
        val imageUrl: String?,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(result: ProductResult): ProductAdminResponse {
                return ProductAdminResponse(
                    id = result.id,
                    brandId = result.brandId,
                    brandName = result.brandName,
                    name = result.name,
                    description = result.description,
                    price = result.price,
                    stockQuantity = result.stockQuantity,
                    likeCount = result.likeCount,
                    status = result.status,
                    displayYn = result.displayYn,
                    imageUrl = result.imageUrl,
                    createdAt = result.createdAt,
                )
            }
        }
    }
}
