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

    fun findByUserId(userId: Long, pageable: Pageable): Page<Coupon>

    fun findByUserIdAndStatus(userId: Long, status: CouponStatus, pageable: Pageable): Page<Coupon>

    fun findByTemplateId(templateId: Long, pageable: Pageable): Page<Coupon>

    fun save(coupon: Coupon): Coupon
}
