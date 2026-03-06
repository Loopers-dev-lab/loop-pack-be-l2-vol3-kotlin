package com.loopers.interfaces.admin.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Sort

enum class AdminProductSortOption(
    val sortOrder: Sort.Order,
) {
    LATEST(Sort.Order.desc("createdAt")),
    PRICE_ASC(Sort.Order.asc("price")),
    ;

    companion object {
        fun fromValue(value: String?): AdminProductSortOption {
            if (value == null) return LATEST

            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                throw CoreException(
                    ErrorType.BAD_REQUEST,
                    "sort는 ${values().joinToString(", ") { it.name }}만 가능합니다",
                )
            }
        }
    }
}
