package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponIssueLimitChecker
import com.loopers.domain.coupon.CouponRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * DB 기반 쿠폰 발급 수량 체커.
 *
 * atomic UPDATE로 수량을 증가시키고, 영향 행이 0이면 수량 초과로 판단.
 * 카프카 파티션 키(couponId)로 순차 처리되므로 락 불필요.
 */
@Component
@ConditionalOnProperty(
    name = ["coupon.limit-checker"],
    havingValue = "db",
    matchIfMissing = true,
)
class DbCouponIssueLimitChecker(
    private val couponRepository: CouponRepository,
) : CouponIssueLimitChecker {

    @Transactional
    override fun tryAcquire(couponId: Long): Boolean {
        return couponRepository.incrementIssuedCount(couponId) > 0
    }

    @Transactional
    override fun release(couponId: Long) {
        // DB 기반에서는 별도 롤백 불필요 — 트랜잭션 롤백으로 처리됨
        // Redis 기반으로 전환 시 INCR 롤백 구현
    }
}
