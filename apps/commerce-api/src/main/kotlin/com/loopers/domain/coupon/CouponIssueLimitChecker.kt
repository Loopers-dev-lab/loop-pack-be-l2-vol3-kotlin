package com.loopers.domain.coupon

/**
 * 쿠폰 발급 수량 제한 체커.
 *
 * 인터페이스로 추상화하여 구현체 교체 시 코드 변경 최소화:
 * - 평상시: DbCouponIssueLimitChecker (atomic UPDATE)
 * - 블랙프라이데이: RedisCouponIssueLimitChecker (DECR) — 설정만 변경
 */
interface CouponIssueLimitChecker {

    /**
     * 발급 가능 여부를 확인하고 수량을 차감한다.
     * @return true면 발급 가능, false면 수량 초과
     */
    fun tryAcquire(couponId: Long): Boolean

    /**
     * 발급 실패 시 수량을 롤백한다.
     */
    fun release(couponId: Long)
}
