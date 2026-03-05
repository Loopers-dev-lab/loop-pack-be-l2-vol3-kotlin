package com.loopers.domain.coupon

import com.loopers.domain.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CouponService(
    private val couponRepository: CouponRepository,
    private val couponIssueRepository: CouponIssueRepository,
) {

    // ──────────────────────────────────────────
    // 쿠폰 템플릿 (Admin)
    // ──────────────────────────────────────────

    @Transactional
    fun createCoupon(command: CreateCouponCommand): Coupon {
        val coupon = Coupon(
            name = command.name,
            type = command.type,
            value = command.value,
            expiredAt = command.expiredAt,
        )
        return couponRepository.save(coupon)
    }

    @Transactional
    fun updateCoupon(couponId: Long, command: UpdateCouponCommand): Coupon {
        val coupon = findCouponById(couponId)
        coupon.update(
            name = command.name,
            type = command.type,
            value = command.value,
            expiredAt = command.expiredAt,
        )
        return coupon
    }

    @Transactional
    fun deleteCoupon(couponId: Long) {
        val coupon = findCouponById(couponId)
        coupon.softDelete()
    }

    /**
     * 쿠폰 단건 조회.
     *
     * @Transactional(readOnly=true) 선택 이유:
     * - 단순 조회지만, JPA 변경 감지(dirty checking)를 비활성화하여 성능 이점을 얻음
     * - 카카오페이 블로그에서 지적한 set_option 오버헤드(6개 추가 쿼리)는 있으나,
     *   readOnly=true는 Hibernate flush 모드를 MANUAL로 설정해 스냅샷 비교를 건너뜀
     * - 단건 조회에서 이 오버헤드가 문제가 되는 트래픽 수준이 아니라면 readOnly 유지가 안전
     */
    @Transactional(readOnly = true)
    fun findCouponById(couponId: Long): Coupon {
        return couponRepository.findById(couponId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 쿠폰입니다.")
    }

    @Transactional(readOnly = true)
    fun findCouponsByIds(ids: List<Long>): List<Coupon> {
        return couponRepository.findByIds(ids)
    }

    @Transactional(readOnly = true)
    fun findAllCoupons(pageable: Pageable): Page<Coupon> {
        return couponRepository.findAll(pageable)
    }

    // ──────────────────────────────────────────
    // 쿠폰 발급 (User)
    // ──────────────────────────────────────────

    /**
     * 쿠폰 발급.
     *
     * @Transactional 선택 이유:
     * - 쿠폰 존재 확인 + 중복 발급 확인 + 저장이 하나의 원자적 작업이어야 함
     * - 중복 발급은 UK(user_id, coupon_id) 제약조건이 최종 방어선 (DB 레벨)
     * - 만료된 쿠폰 발급 방지는 시간 비교로 처리 (스케줄러 의존 X)
     */
    @Transactional
    fun issueCoupon(couponId: Long, userId: Long): CouponIssue {
        val coupon = findCouponById(couponId)

        if (coupon.isExpired()) {
            throw CoreException(ErrorType.BAD_REQUEST, "만료된 쿠폰은 발급할 수 없습니다.")
        }

        val existing = couponIssueRepository.findByUserIdAndCouponId(userId, couponId)
        if (existing != null) {
            throw CoreException(ErrorType.CONFLICT, "이미 발급받은 쿠폰입니다.")
        }

        val couponIssue = CouponIssue(couponId = couponId, userId = userId)
        return couponIssueRepository.save(couponIssue)
    }

    /**
     * 주문에 쿠폰을 적용한다.
     *
     * 소유권 검증 → 사용 가능 여부 → 만료 여부 → 할인 계산 → 사용 처리를 원자적으로 수행.
     * 쿠폰 사용에 대한 비즈니스 로직을 캡슐화하여, Facade는 결과(할인 금액)만 받아서 조합한다.
     *
     * @Transactional 선택 이유:
     * - 쿠폰 검증 + use() 상태 변경이 하나의 원자적 작업
     * - Facade의 @Transactional에서 호출 시 REQUIRED 전파로 합류
     *
     * 동시성 제어 — @Version 낙관적 락:
     * - CouponIssue에 @Version 필드 추가 → dirty checking 시 WHERE version=? 조건 자동 부여
     * - 같은 유저의 중복 요청(더블클릭) 시 두 번째 요청은 OptimisticLockingFailureException 발생
     * - 경합이 극히 낮으므로(같은 유저뿐) 재시도 로직 불필요 → 바로 실패 처리
     */
    @Transactional
    fun useCouponForOrder(couponIssueId: Long, userId: Long, orderAmount: Money): CouponUsageInfo {
        val couponIssue = findCouponIssueById(couponIssueId)

        if (couponIssue.userId != userId) {
            throw CoreException(ErrorType.FORBIDDEN, "본인의 쿠폰만 사용할 수 있습니다.")
        }
        if (!couponIssue.isUsable()) {
            throw CoreException(ErrorType.BAD_REQUEST, "사용할 수 없는 쿠폰입니다.")
        }

        val coupon = findCouponById(couponIssue.couponId)
        if (coupon.isExpired()) {
            throw CoreException(ErrorType.BAD_REQUEST, "만료된 쿠폰입니다.")
        }

        val discountAmount = coupon.calculateDiscount(orderAmount)
        couponIssue.use()

        return CouponUsageInfo(couponIssueId = couponIssue.id, discountAmount = discountAmount)
    }

    @Transactional(readOnly = true)
    fun findAllByUserId(userId: Long): List<CouponIssue> {
        return couponIssueRepository.findAllByUserId(userId)
    }

    @Transactional(readOnly = true)
    fun findAllByCouponId(couponId: Long, pageable: Pageable): Page<CouponIssue> {
        return couponIssueRepository.findAllByCouponId(couponId, pageable)
    }

    @Transactional(readOnly = true)
    fun findCouponIssueById(couponIssueId: Long): CouponIssue {
        return couponIssueRepository.findById(couponIssueId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 발급 쿠폰입니다.")
    }
}
