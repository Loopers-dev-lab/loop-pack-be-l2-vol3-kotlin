package com.loopers.interfaces.api.product

import org.springframework.data.domain.Sort

enum class ProductSortType(val sort: Sort) {
    PRICE_ASC(Sort.by(Sort.Direction.ASC, "price")),
    LIKES_DESC(Sort.by(Sort.Direction.DESC, "likeCount")),
    LATEST(Sort.by(Sort.Direction.DESC, "createdAt")),
}
