package com.loopers.domain.coupon

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CouponRepository {

    fun findById(id: Long): Coupon?

    fun findByUserIdAndTemplateId(userId: Long, templateId: Long): Coupon?

    /**
     * 사용자의 쿠폰을 행 락과 함께 조회 (동시성 제어)
     * 중복 발급 검사에 사용됨
     */
    fun findByUserIdAndTemplateIdForUpdate(userId: Long, templateId: Long): Coupon?

    /**
     * 쿠폰 상태를 원자적으로 ISSUED에서 USED로 변경
     * ISSUED 상태일 때만 USED로 업데이트됨 (동시성 제어)
     *
     * @return 업데이트된 행 수 (0 또는 1)
     *   - 1: 성공적으로 USED로 변경됨
     *   - 0: 이미 USED 상태이거나 쿠폰이 없음
     */
    fun updateStatusToUsed(couponId: Long): Int

    fun findByUserId(userId: Long, pageable: Pageable): Page<Coupon>

    fun findByUserIdAndStatus(userId: Long, status: CouponStatus, pageable: Pageable): Page<Coupon>

    fun findByTemplateId(templateId: Long, pageable: Pageable): Page<Coupon>

    fun save(coupon: Coupon): Coupon
}
