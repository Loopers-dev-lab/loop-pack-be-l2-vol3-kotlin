package com.loopers.domain.payment

enum class ReceiptStatus {
    PENDING, // PG 요청 완료, 콜백 대기 중
    COMPLETED, // 결제 완료
    FAILED, // 결제 실패
    CANCELLED, // 결제 취소
    TIMEOUT, // 콜백 타임아웃 (재시도 가능)
}
