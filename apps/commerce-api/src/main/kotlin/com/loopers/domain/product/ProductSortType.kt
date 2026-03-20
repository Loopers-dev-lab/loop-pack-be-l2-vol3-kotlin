package com.loopers.domain.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

enum class ProductSortType(val value: String) {
    LATEST("latest"),
    PRICE_ASC("price_asc"),
    LIKES_DESC("likes_desc"),
    ;

    fun apply(pageable: Pageable): Pageable = PageRequest.of(pageable.pageNumber, pageable.pageSize, sort)

    private val sort: Sort
        get() = when (this) {
            LATEST -> Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id"),
            )

            PRICE_ASC -> Sort.by(
                Sort.Order.asc("price"),
                Sort.Order.desc("id"),
            )

            LIKES_DESC -> Sort.by(
                Sort.Order.desc("likesCount"),
                Sort.Order.desc("id"),
            )
        }

    companion object {
        fun from(value: String): ProductSortType = entries.firstOrNull { it.value == value }
                ?: throw CoreException(
                    ErrorType.BAD_REQUEST,
                    "지원하지 않는 상품 정렬 조건입니다: $value (allowed: ${entries.joinToString { it.value }})",
                )
    }
}
