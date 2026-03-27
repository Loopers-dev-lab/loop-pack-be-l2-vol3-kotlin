package com.loopers.domain.coupon

interface CouponTemplateRepository {

    fun findById(id: Long): CouponTemplateDto?

    /**
     * 행 락과 함께 조회 (동시성 제어)
     * 수량 확인 + 쿠폰 발급을 직렬화하기 위해 사용
     */
    fun findByIdForUpdate(id: Long): CouponTemplateDto?

    fun save(couponTemplate: CouponTemplateDto): CouponTemplateDto

    fun incrementIssuedCountIfAvailable(id: Long): Int
}
