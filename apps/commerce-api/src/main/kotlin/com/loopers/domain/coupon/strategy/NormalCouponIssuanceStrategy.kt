package com.loopers.domain.coupon.strategy

/**
 * 일반 쿠폰 발급 전략
 *
 * totalCount가 없는 쿠폰 (수량 무제한)에 적용
 * - 토픽: coupon-normal-events
 * - 파티션 키: "normal:templateId" (같은 템플릿의 모든 요청 → 같은 파티션)
 */
class NormalCouponIssuanceStrategy : CouponIssuanceStrategy {
    override fun getTopic(): String = "coupon-normal-events"

    override fun getPartitionKey(templateId: Long): String = "normal:$templateId"
}
