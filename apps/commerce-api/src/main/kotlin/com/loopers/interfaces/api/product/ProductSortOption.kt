package com.loopers.interfaces.api.product

import org.springframework.data.domain.Sort

enum class ProductSortOption(
    val sortOrder: Sort.Order,
) {
    LIKE_COUNT(Sort.Order.desc("likeCount")),
    CREATED_AT(Sort.Order.desc("createdAt")),
    PRICE(Sort.Order.asc("price")),
    ;

    companion object {
        fun fromValue(value: String?): ProductSortOption =
            value?.uppercase()?.let {
                try {
                    valueOf(it)
                } catch (e: IllegalArgumentException) {
                    CREATED_AT
                }
            } ?: CREATED_AT
    }
}
