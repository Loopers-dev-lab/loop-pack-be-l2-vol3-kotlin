package com.loopers.domain.order

enum class OrderStatus {
    // 결제 단계
    PENDING_PAYMENT,    // 결제 대기
    PAID,               // 결제 완료

    // 배송 단계
    PREPARING,          // 배송 준비
    SHIPPED,            // 배송 중
    DELIVERED,          // 배송 완료
    COMPLETED,          // 구매 확정

    // 취소 단계
    CANCELLED,

    // 교환 단계
    EXCHANGE_REQUESTED, // 교환 접수
    EXCHANGING,         // 교환 상품 수거 및 검수 중
    EXCHANGED,          // 교환 상품 배송 완료

    // 환불 단계
    RETURN_REQUESTED,   // 반품 접수
    RETURNING,         // 반품 상품 수거 및 검수 중
    RETURNED    // 반품 및 환불 완료
}
