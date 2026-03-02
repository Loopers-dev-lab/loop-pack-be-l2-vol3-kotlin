package com.loopers.interfaces.api.product

import com.loopers.application.product.ProductResult
import com.loopers.domain.Money
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.time.ZonedDateTime

class ProductV1Dto {

    data class GetProductsRequest(
        val brandId: Long? = null,
        val sort: String = "latest",
        val page: Int = 0,
        val size: Int = 20,
    ) {
        fun toPageable(): Pageable {
            val sortOrder = ProductSortType.from(sort).sort
            return PageRequest.of(page, size, sortOrder)
        }
    }

    data class ProductResponse(
        val id: Long,
        val brandName: String,
        val name: String,
        val description: String?,
        val price: Money,
        val likeCount: Int,
        val soldOut: Boolean,
        val imageUrl: String?,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(result: ProductResult): ProductResponse {
                return ProductResponse(
                    id = result.id,
                    brandName = result.brandName,
                    name = result.name,
                    description = result.description,
                    price = result.price,
                    likeCount = result.likeCount,
                    soldOut = result.stockQuantity <= 0,
                    imageUrl = result.imageUrl,
                    createdAt = result.createdAt,
                )
            }
        }
    }
}
