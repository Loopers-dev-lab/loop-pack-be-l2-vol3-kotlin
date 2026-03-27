package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponIssuanceResult
import com.loopers.domain.coupon.CouponIssuanceResultRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CouponIssuanceResultJpaRepository : JpaRepository<CouponIssuanceResult, String>

@Repository
class CouponIssuanceResultRepositoryImpl(
    private val jpaRepository: CouponIssuanceResultJpaRepository,
) : CouponIssuanceResultRepository {
    override fun save(result: CouponIssuanceResult): CouponIssuanceResult {
        return jpaRepository.save(result)
    }

    override fun findByDedupeKey(dedupeKey: String): CouponIssuanceResult? {
        return jpaRepository.findById(dedupeKey).orElse(null)
    }
}
