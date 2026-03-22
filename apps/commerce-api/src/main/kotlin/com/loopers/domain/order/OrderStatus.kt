package com.loopers.domain.order

enum class OrderStatus {
    PENDING, // 결제 대기
    PAYMENT_REQUESTED, // 결제 요청 완료
    PAID, // 결제완료
    SHIPPED, // 배송중
    DELIVERED, // 배송완료
    RETURNED, // 반품
}
