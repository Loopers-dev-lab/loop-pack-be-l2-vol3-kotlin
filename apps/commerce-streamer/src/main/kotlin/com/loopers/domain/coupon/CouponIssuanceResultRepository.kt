package com.loopers.domain.coupon

interface CouponIssuanceResultRepository {

    fun findByDedupeKey(dedupeKey: String): CouponIssuanceResultDto?

    fun save(result: CouponIssuanceResultDto): CouponIssuanceResultDto
}
