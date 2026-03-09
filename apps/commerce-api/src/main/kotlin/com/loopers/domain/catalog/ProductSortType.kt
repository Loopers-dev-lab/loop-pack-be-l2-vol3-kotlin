package com.loopers.domain.catalog

import org.springframework.data.domain.Sort

enum class ProductSortType {
    LATEST,
    PRICE_ASC,
    ;

    fun toSort(): Sort {
        return when (this) {
            LATEST -> Sort.by(Sort.Direction.DESC, "id")
            PRICE_ASC -> Sort.by(Sort.Direction.ASC, "price")
        }
    }
}
