package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponInfo
import com.loopers.domain.coupon.CouponIssueInfo
import com.loopers.domain.coupon.CouponIssueRequest
import com.loopers.domain.coupon.CouponIssueRequestService
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.CreateCouponCommand
import com.loopers.domain.coupon.UpdateCouponCommand
import com.loopers.domain.coupon.event.CouponEvent
import com.loopers.domain.event.DomainEventPublisher
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CouponFacade(
    private val couponService: CouponService,
    private val couponIssueRequestService: CouponIssueRequestService,
    private val domainEventPublisher: DomainEventPublisher,
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

    // ──────────────────────────────────────────
    // User: 선착순 쿠폰 발급 요청
    // ──────────────────────────────────────────

    /**
     * 선착순 쿠폰 발급 요청.
     *
     * API는 Kafka에 발행만 하고, 실제 발급은 Consumer(streamer)가 처리한다.
     * → 트래픽이 몰려도 Kafka가 버퍼 역할을 하여 시스템 보호
     *
     * Outbox 패턴 적용:
     * - 요청 저장 + Outbox 저장이 같은 트랜잭션에서 원자적으로 처리
     * - Kafka 발행 실패 시 OutboxEventRelay가 5초 간격으로 재시도 (At Least Once)
     * - 직접 KafkaTemplate 발행 시 발생하던 유실 위험 제거
     */
    @Transactional
    fun requestCouponIssue(couponId: Long, userId: Long): CouponIssueRequestResult {
        // 쿠폰 존재 + 만료 확인
        val coupon = couponService.findCouponById(couponId)
        if (coupon.isExpired()) {
            throw CoreException(ErrorType.BAD_REQUEST, "만료된 쿠폰입니다.")
        }

        // 중복 요청 확인
        val existing = couponIssueRequestService.findByUserIdAndCouponId(userId, couponId)
        if (existing != null) {
            throw CoreException(ErrorType.CONFLICT, "이미 발급 요청한 쿠폰입니다.")
        }

        // 요청 저장
        val request = CouponIssueRequest(userId = userId, couponId = couponId)
        val saved = couponIssueRequestService.save(request)

        // 이벤트 발행 → BEFORE_COMMIT에서 Outbox에 저장 → 릴레이가 Kafka 발행
        domainEventPublisher.publish(
            CouponEvent.IssueRequested(
                aggregateId = couponId,
                requestId = saved.requestId,
                userId = userId,
            ),
        )

        return CouponIssueRequestResult.from(saved)
    }

    /**
     * 선착순 쿠폰 발급 결과 조회 (Polling).
     */
    fun getIssueRequestStatus(requestId: String): CouponIssueRequestResult {
        val request = couponIssueRequestService.findByRequestId(requestId)
        return CouponIssueRequestResult.from(request)
    }
}
