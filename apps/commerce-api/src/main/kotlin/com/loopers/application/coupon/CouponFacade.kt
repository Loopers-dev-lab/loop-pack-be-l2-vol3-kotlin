package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponInfo
import com.loopers.domain.coupon.CouponIssueInfo
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.CreateCouponCommand
import com.loopers.domain.coupon.UpdateCouponCommand
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class CouponFacade(
    private val couponService: CouponService,
) {

    // ──────────────────────────────────────────
    // Admin: 쿠폰 템플릿 관리
    // ──────────────────────────────────────────

    fun createCoupon(criteria: CreateCouponCriteria): CouponResult {
        val command = CreateCouponCommand(
            name = criteria.name,
            type = CouponType.valueOf(criteria.type),
            value = criteria.value,
            expiredAt = criteria.expiredAt,
        )
        val coupon = couponService.createCoupon(command)
        return CouponResult.from(CouponInfo.from(coupon))
    }

    fun updateCoupon(couponId: Long, criteria: UpdateCouponCriteria): CouponResult {
        val command = UpdateCouponCommand(
            name = criteria.name,
            type = CouponType.valueOf(criteria.type),
            value = criteria.value,
            expiredAt = criteria.expiredAt,
        )
        val coupon = couponService.updateCoupon(couponId, command)
        return CouponResult.from(CouponInfo.from(coupon))
    }

    fun deleteCoupon(couponId: Long) {
        couponService.deleteCoupon(couponId)
    }

    fun getCoupon(couponId: Long): CouponResult {
        val coupon = couponService.findCouponById(couponId)
        return CouponResult.from(CouponInfo.from(coupon))
    }

    fun getCoupons(pageable: Pageable): Page<CouponResult> {
        return couponService.findAllCoupons(pageable)
            .map { CouponResult.from(CouponInfo.from(it)) }
    }

    fun getCouponIssues(couponId: Long, pageable: Pageable): Page<CouponIssueResult> {
        return couponService.findAllByCouponId(couponId, pageable)
            .map { CouponIssueResult.from(CouponIssueInfo.from(it)) }
    }

    // ──────────────────────────────────────────
    // User: 쿠폰 발급 & 내 쿠폰 조회
    // ──────────────────────────────────────────

    fun issueCoupon(couponId: Long, userId: Long): CouponIssueResult {
        val couponIssue = couponService.issueCoupon(couponId, userId)
        return CouponIssueResult.from(CouponIssueInfo.from(couponIssue))
    }

    /**
     * 내 쿠폰 목록 조회.
     *
     * Facade 역할이 드러나는 메서드:
     * - CouponIssue(발급 정보) + Coupon(템플릿 정보)을 조합
     * - 만료 여부를 시간 기반으로 보정 (MyCouponResult.resolveStatus)
     *
     * @Transactional 미적용 이유:
     * - 이 메서드는 순수 조회이고, Service 메서드들이 각각 readOnly 트랜잭션을 가짐
     * - Facade에서 추가 트랜잭션을 여는 것은 불필요한 오버헤드
     * - 카카오페이 블로그: 조회만 하는 곳에서 트랜잭션을 열면 set_option 쿼리 6개 추가
     * - 다만, 하나의 트랜잭션에서 일관된 스냅샷이 필요하면 readOnly 적용 고려
     *   (현재는 쿠폰 목록 조회에서 그 수준의 일관성은 불필요)
     */
    fun getMyCoupons(userId: Long): List<MyCouponResult> {
        val issues = couponService.findAllByUserId(userId)
        if (issues.isEmpty()) return emptyList()

        val couponIds = issues.map { it.couponId }.distinct()
        val coupons = couponService.findCouponsByIds(couponIds).associateBy { it.id }

        return issues.mapNotNull { issue ->
            val coupon = coupons[issue.couponId] ?: return@mapNotNull null
            MyCouponResult.of(
                info = CouponIssueInfo.from(issue),
                couponInfo = CouponInfo.from(coupon),
            )
        }
    }
}
