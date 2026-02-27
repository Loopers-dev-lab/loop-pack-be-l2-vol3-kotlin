package com.loopers.interfaces.admin.order

import org.springframework.data.domain.Sort

enum class AdminOrderSortOption(val sortOrder: Sort.Order) {
    ORDER_DATE_DESC(Sort.Order(Sort.Direction.DESC, "createdAt")),
    ORDER_DATE_ASC(Sort.Order(Sort.Direction.ASC, "createdAt")),
    TOTAL_PRICE_DESC(Sort.Order(Sort.Direction.DESC, "totalPrice")),
    TOTAL_PRICE_ASC(Sort.Order(Sort.Direction.ASC, "totalPrice")),
    ;

    companion object {
        fun fromValue(value: String?): AdminOrderSortOption {
            return when (value?.uppercase()) {
                "ORDER_DATE_ASC" -> ORDER_DATE_ASC
                "TOTAL_PRICE_DESC" -> TOTAL_PRICE_DESC
                "TOTAL_PRICE_ASC" -> TOTAL_PRICE_ASC
                else -> ORDER_DATE_DESC
            }
        }
    }
}
