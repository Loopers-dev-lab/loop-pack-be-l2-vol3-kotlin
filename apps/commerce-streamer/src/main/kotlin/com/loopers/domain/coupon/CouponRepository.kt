package com.loopers.domain.coupon

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CouponRepository {

    fun findById(id: Long): CouponDto?

    fun findByUserIdAndTemplateId(userId: Long, templateId: Long): CouponDto?

    /**
     * 사용자의 쿠폰을 행 락과 함께 조회 (동시성 제어)
     * 중복 발급 검사에 사용됨
     */
    fun findByUserIdAndTemplateIdForUpdate(userId: Long, templateId: Long): CouponDto?

    fun findByUserId(userId: Long, pageable: Pageable): Page<CouponDto>

    fun findByTemplateId(templateId: Long, pageable: Pageable): Page<CouponDto>

    fun save(coupon: CouponDto): CouponDto
}
