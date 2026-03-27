package com.loopers.domain.coupon.strategy

/**
 * 선착순 쿠폰 발급 전략
 *
 * totalCount가 설정된 쿠폰 (수량 제한)에 적용
 * - 토픽: coupon-limited-events
 * - 파티션 키: "limited:templateId" (같은 템플릿의 모든 요청 → 같은 파티션)
 */
class LimitedCouponIssuanceStrategy : CouponIssuanceStrategy {
    override fun getTopic(): String = "coupon-limited-events"

    override fun getPartitionKey(templateId: Long): String = "limited:$templateId"
}
