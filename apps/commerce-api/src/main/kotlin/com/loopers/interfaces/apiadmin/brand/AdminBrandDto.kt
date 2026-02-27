package com.loopers.interfaces.apiadmin.brand

import com.loopers.application.brand.BrandInfo
import com.loopers.support.common.PageResult
import java.time.ZonedDateTime

class AdminBrandDto {
    data class PageResponse(
        val content: List<ListItem>,
        val page: Int,
        val size: Int,
        val totalElements: Long,
        val totalPages: Int,
    ) {
        companion object {
            fun from(page: PageResult<BrandInfo>): PageResponse {
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
    ) {
        companion object {
            fun from(info: BrandInfo): ListItem {
                return ListItem(
                    id = info.id,
                    name = info.name,
                    description = info.description,
                )
            }
        }
    }

    data class CreateRequest(
        val name: String,
        val description: String?,
    )

    data class CreateResponse(
        val id: Long,
        val name: String,
        val description: String?,
    ) {
        companion object {
            fun from(info: BrandInfo): CreateResponse {
                return CreateResponse(
                    id = info.id,
                    name = info.name,
                    description = info.description,
                )
            }
        }
    }

    data class UpdateRequest(
        val name: String,
        val description: String?,
    )

    data class DetailResponse(
        val id: Long,
        val name: String,
        val description: String?,
        val createdAt: ZonedDateTime,
        val updatedAt: ZonedDateTime,
    ) {
        companion object {
            fun from(info: BrandInfo): DetailResponse {
                return DetailResponse(
                    id = info.id,
                    name = info.name,
                    description = info.description,
                    createdAt = info.createdAt,
                    updatedAt = info.updatedAt,
                )
            }
        }
    }
}
