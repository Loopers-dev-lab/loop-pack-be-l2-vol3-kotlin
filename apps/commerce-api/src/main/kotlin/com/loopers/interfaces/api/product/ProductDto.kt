package com.loopers.interfaces.api.product

import com.loopers.application.product.ProductDetailInfo
import com.loopers.application.product.ProductInfo
import com.loopers.support.common.PageResult

class ProductDto {
    data class DetailResponse(
        val id: Long,
        val name: String,
        val price: Long,
        val description: String?,
        val brandId: Long,
        val brandName: String,
        val likeCount: Int,
    ) {
        companion object {
            fun from(info: ProductDetailInfo): DetailResponse {
                return DetailResponse(
                    id = info.id,
                    name = info.name,
                    price = info.price,
                    description = info.description,
                    brandId = info.brandId,
                    brandName = info.brandName,
                    likeCount = info.likeCount,
                )
            }
        }
    }

    data class PageResponse(
        val content: List<ListItem>,
        val page: Int,
        val size: Int,
        val totalElements: Long,
        val totalPages: Int,
    ) {
        companion object {
            fun from(page: PageResult<ProductInfo>): PageResponse {
                return PageResponse(
                    content = page.content.map { ListItem.from(it) },
                    page = page.page,
                    size = page.size,
                    totalElements = page.totalElements,
                    totalPages = page.totalPages,
                )
            }
        }
    }

    data class ListItem(
        val id: Long,
        val name: String,
        val description: String?,
        val price: Long,
        val likes: Int,
        val stockQuantity: Int,
        val brandId: Long,
    ) {
        companion object {
            fun from(info: ProductInfo): ListItem {
                return ListItem(
                    id = info.id,
                    name = info.name,
                    description = info.description,
                    price = info.price,
                    likes = info.likes,
                    stockQuantity = info.stockQuantity,
                    brandId = info.brandId,
                )
            }
        }
    }
}
