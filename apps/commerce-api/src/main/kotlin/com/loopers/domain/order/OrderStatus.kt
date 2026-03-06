package com.loopers.domain.order

enum class OrderStatus {
    PENDING, // 주문 접수
    PAID, // 결제완료
    SHIPPED, // 배송중
    DELIVERED, // 배송완료
    RETURNED, // 반품
}
