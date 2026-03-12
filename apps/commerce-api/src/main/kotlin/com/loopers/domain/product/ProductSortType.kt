package com.loopers.domain.product

import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.domain.Sort.Direction.DESC

enum class ProductSortType(
    val fieldName: String,
    val direction: Sort.Direction,
) {
    LATEST("createdAt", DESC),
    PRICE_ASC("price", ASC),
    LIKES_DESC("likeCount", DESC),
    ;

    fun toSort(): Sort = Sort.by(direction, fieldName)
}
