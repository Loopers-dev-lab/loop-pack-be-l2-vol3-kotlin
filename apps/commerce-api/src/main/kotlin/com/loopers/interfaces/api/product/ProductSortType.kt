package com.loopers.interfaces.api.product

import org.springframework.data.domain.Sort

enum class ProductSortType(val sort: Sort) {
    LATEST(Sort.by("createdAt").descending()),
    PRICE_ASC(Sort.by("price").ascending()),
    PRICE_DESC(Sort.by("price").descending()),
    LIKES_ASC(Sort.by("likeCount").ascending()),
    LIKES_DESC(Sort.by("likeCount").descending()),
    ;

    companion object {
        fun from(value: String): ProductSortType {
            return entries.find { it.name.equals(value, ignoreCase = true) }
                ?: LATEST
        }
    }
}
