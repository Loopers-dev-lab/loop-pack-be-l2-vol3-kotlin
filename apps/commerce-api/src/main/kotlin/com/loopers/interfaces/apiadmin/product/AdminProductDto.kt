package com.loopers.interfaces.apiadmin.product

import com.loopers.application.product.AdminProductInfo
import com.loopers.support.common.PageResult
import java.time.ZonedDateTime

class AdminProductDto {
    data class PageResponse(
        val content: List<ListItem>,
        val page: Int,
        val size: Int,
        val totalElements: Long,
        val totalPages: Int,
    ) {
        companion object {
            fun from(page: PageResult<AdminProductInfo>): PageResponse {
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
        val brandId: Long,
        val brandName: String,
        val stockQuantity: Int,
        val likeCount: Int,
        val createdAt: ZonedDateTime,
        val updatedAt: ZonedDateTime,
    ) {
        companion object {
            fun from(info: AdminProductInfo): ListItem {
                return ListItem(
                    id = info.id,
                    name = info.name,
                    description = info.description,
                    price = info.price,
                    brandId = info.brandId,
                    brandName = info.brandName,
                    stockQuantity = info.stockQuantity,
                    likeCount = info.likeCount,
                    createdAt = info.createdAt,
                    updatedAt = info.updatedAt,
                )
            }
        }
    }

    data class DetailResponse(
        val id: Long,
        val name: String,
        val description: String?,
        val price: Long,
        val brandId: Long,
        val brandName: String,
        val stockQuantity: Int,
        val likeCount: Int,
        val createdAt: ZonedDateTime,
        val updatedAt: ZonedDateTime,
    ) {
        companion object {
            fun from(info: AdminProductInfo): DetailResponse {
                return DetailResponse(
                    id = info.id,
                    name = info.name,
                    description = info.description,
                    price = info.price,
                    brandId = info.brandId,
                    brandName = info.brandName,
                    stockQuantity = info.stockQuantity,
                    likeCount = info.likeCount,
                    createdAt = info.createdAt,
                    updatedAt = info.updatedAt,
                )
            }
        }
    }

    data class UpdateRequest(
        val name: String,
        val description: String?,
        val price: Long,
        val stockQuantity: Int,
        val brandId: Long,
    )

    data class CreateRequest(
        val name: String,
        val description: String?,
        val price: Long,
        val stockQuantity: Int,
        val brandId: Long,
    )

    data class CreateResponse(
        val id: Long,
        val name: String,
        val description: String?,
        val price: Long,
        val stockQuantity: Int,
        val brandId: Long,
        val brandName: String,
        val likeCount: Int,
        val createdAt: ZonedDateTime,
        val updatedAt: ZonedDateTime,
    ) {
        companion object {
            fun from(info: AdminProductInfo): CreateResponse {
                return CreateResponse(
                    id = info.id,
                    name = info.name,
                    description = info.description,
                    price = info.price,
                    stockQuantity = info.stockQuantity,
                    brandId = info.brandId,
                    brandName = info.brandName,
                    likeCount = info.likeCount,
                    createdAt = info.createdAt,
                    updatedAt = info.updatedAt,
                )
            }
        }
    }
}
