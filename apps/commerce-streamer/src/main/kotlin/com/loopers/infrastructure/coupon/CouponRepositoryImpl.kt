package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.model.Coupon
import com.loopers.domain.coupon.repository.CouponRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.stereotype.Repository
import java.util.Optional

interface CouponJpaRepository : JpaRepository<CouponEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithLockById(id: Long): Optional<CouponEntity>
}

@Repository
class CouponRepositoryImpl(
    private val couponJpaRepository: CouponJpaRepository,
) : CouponRepository {

    override fun findById(id: Long): Coupon? =
        couponJpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByIdForUpdate(id: Long): Coupon? =
        couponJpaRepository.findWithLockById(id).orElse(null)?.toDomain()

    override fun save(coupon: Coupon): Coupon {
        val entity = couponJpaRepository.findById(coupon.id)
            .orElseThrow { CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다. id=${coupon.id}") }
        entity.issuedCount = coupon.issuedCount
        return couponJpaRepository.save(entity).toDomain()
    }
}
