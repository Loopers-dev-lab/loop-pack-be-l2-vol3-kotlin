package com.loopers.interfaces.api.product

import org.springframework.data.domain.Sort

enum class ProductSortOption(
    val sortOrder: Sort.Order,
) {
    LATEST(Sort.Order.asc("createdAt")),
    PRICE_ASC(Sort.Order.asc("price")),
    ;

    companion object {
        fun fromValue(value: String?): ProductSortOption =
            value?.uppercase()?.let {
                try {
                    valueOf(it)
                } catch (e: IllegalArgumentException) {
                    LATEST
                }
            } ?: LATEST
    }
}
