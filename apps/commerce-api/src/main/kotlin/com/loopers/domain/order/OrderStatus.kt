package com.loopers.domain.order

enum class OrderStatus {
    ORDERED,
    CONFIRMED,
    SHIPPING,
    DELIVERED,
    CANCELLED,
    ;

    fun canTransitionTo(next: OrderStatus): Boolean {
        return when (this) {
            ORDERED -> next == CONFIRMED || next == CANCELLED
            CONFIRMED -> next == SHIPPING || next == CANCELLED
            SHIPPING -> next == DELIVERED || next == CANCELLED
            DELIVERED -> false
            CANCELLED -> false
        }
    }
}
