package com.loopers.domain.coupon

enum class IssuanceStatus {
    PENDING, // 처리 대기 중
    ISSUED, // 발급 완료
    SOLD_OUT, // 품절
    DUPLICATE, // 중복 발급
    TEMPLATE_EXPIRED, // 템플릿 만료
    TEMPLATE_NOT_FOUND, // 템플릿 없음
}
