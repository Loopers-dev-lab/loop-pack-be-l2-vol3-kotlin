package com.loopers.interfaces.apiadmin.coupon

import com.loopers.application.coupon.CouponInfo
import com.loopers.support.common.PageResult
import java.time.ZonedDateTime

class AdminCouponDto {
    data class PageResponse(
        val content: List<ListItem>,
        val page: Int,
        val size: Int,
        val totalElements: Long,
        val totalPages: Int,
    ) {
        companion object {
            fun from(page: PageResult<CouponInfo>): PageResponse {
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

    data class DetailResponse(
        val id: Long,
        val name: String,
        val discountType: String,
        val discountValue: Long,
        val totalQuantity: Int,
        val issuedQuantity: Int,
        val expiresAt: ZonedDateTime,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(info: CouponInfo): DetailResponse {
                return DetailResponse(
                    id = info.id,
                    name = info.name,
                    discountType = info.discountType.name,
                    discountValue = info.discountValue,
                    totalQuantity = info.totalQuantity,
                    issuedQuantity = info.issuedQuantity,
                    expiresAt = info.expiresAt,
                    createdAt = info.createdAt,
                )
            }
        }
    }

    data class ListItem(
        val id: Long,
        val name: String,
        val discountType: String,
        val discountValue: Long,
        val totalQuantity: Int,
        val issuedQuantity: Int,
        val expiresAt: ZonedDateTime,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(info: CouponInfo): ListItem {
                return ListItem(
                    id = info.id,
                    name = info.name,
                    discountType = info.discountType.name,
                    discountValue = info.discountValue,
                    totalQuantity = info.totalQuantity,
                    issuedQuantity = info.issuedQuantity,
                    expiresAt = info.expiresAt,
                    createdAt = info.createdAt,
                )
            }
        }
    }
}
