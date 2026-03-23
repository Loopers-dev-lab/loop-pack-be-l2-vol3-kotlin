package com.loopers.domain.payment

enum class PaymentStatus {
    /** PG에 결제 요청을 보낸 상태 */
    REQUESTED,

    /** PG에서 결제 승인 완료 */
    APPROVED,

    /** PG에서 결제 거절 (한도초과, 잘못된 카드 등) */
    REJECTED,

    /** PG 요청 타임아웃 (실제 처리됐을 수 있어 폴링 확인 필요) */
    TIMEOUT,
}
