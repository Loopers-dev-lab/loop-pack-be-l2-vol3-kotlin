package com.loopers.domain.coupon.strategy

/**
 * 쿠폰 발급 전략
 *
 * 선착순 쿠폰과 일반 쿠폰을 구분하여 처리하기 위한 전략 패턴
 */
interface CouponIssuanceStrategy {
    /**
     * Kafka 토픽 반환
     *
     * @return 쿠폰 발급 요청을 보낼 Kafka 토픽
     */
    fun getTopic(): String

    /**
     * Kafka 파티션 키 반환
     *
     * @param templateId 쿠폰 템플릿 ID
     * @return 같은 templateId의 모든 요청을 같은 파티션으로 라우팅하는 key
     *         (형식: "type:templateId")
     */
    fun getPartitionKey(templateId: Long): String
}
