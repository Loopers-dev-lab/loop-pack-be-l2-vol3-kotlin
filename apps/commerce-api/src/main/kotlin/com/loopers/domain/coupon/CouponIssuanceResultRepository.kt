package com.loopers.domain.coupon

interface CouponIssuanceResultRepository {
    fun save(result: CouponIssuanceResult): CouponIssuanceResult
    fun findByDedupeKey(dedupeKey: String): CouponIssuanceResult?
}
