package com.loopers.application.api.coupon

import com.loopers.domain.coupon.CouponTemplate
import com.loopers.domain.coupon.strategy.CouponIssuanceStrategy
import com.loopers.domain.coupon.strategy.LimitedCouponIssuanceStrategy
import com.loopers.domain.coupon.strategy.NormalCouponIssuanceStrategy
import org.springframework.stereotype.Component

/**
 * 쿠폰 템플릿의 특성에 따라 발급 전략을 선택
 *
 * - totalCount가 있음 → LimitedCouponIssuanceStrategy (선착순)
 * - totalCount가 없음 → NormalCouponIssuanceStrategy (일반)
 */
@Component
class CouponIssuanceStrategySelector {
    fun select(template: CouponTemplate): CouponIssuanceStrategy {
        return if (template.totalCount != null) {
            LimitedCouponIssuanceStrategy()
        } else {
            NormalCouponIssuanceStrategy()
        }
    }
}
