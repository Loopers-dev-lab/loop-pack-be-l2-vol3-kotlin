package com.loopers.infrastructure.coupon

import com.loopers.domain.PageResult
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.coupon.model.IssuedCoupon
import com.loopers.domain.coupon.repository.IssuedCouponRepository
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.stereotype.Repository

interface IssuedCouponJpaRepository : JpaRepository<IssuedCouponEntity, Long> {
    fun findByRefCouponIdAndRefUserId(couponId: Long, refUserId: Long): IssuedCouponEntity?

    fun findAllByRefUserId(refUserId: Long): List<IssuedCouponEntity>

    fun findAllByRefCouponId(couponId: Long, pageable: Pageable): Page<IssuedCouponEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithLockById(id: Long): IssuedCouponEntity?
}

@Repository
class IssuedCouponRepositoryImpl(
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
) : IssuedCouponRepository {

    override fun save(issuedCoupon: IssuedCoupon): IssuedCoupon =
        issuedCouponJpaRepository.save(IssuedCouponEntity.fromDomain(issuedCoupon)).toDomain()

    override fun findById(id: Long): IssuedCoupon? =
        issuedCouponJpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByIdForUpdate(id: Long): IssuedCoupon? =
        issuedCouponJpaRepository.findWithLockById(id)?.toDomain()

    override fun findByRefCouponIdAndRefUserId(couponId: Long, userId: UserId): IssuedCoupon? =
        issuedCouponJpaRepository.findByRefCouponIdAndRefUserId(couponId, userId.value)?.toDomain()

    override fun findAllByRefUserId(userId: UserId): List<IssuedCoupon> =
        issuedCouponJpaRepository.findAllByRefUserId(userId.value).map { it.toDomain() }

    override fun findAllByRefCouponId(couponId: Long, page: Int, size: Int): PageResult<IssuedCoupon> {
        val pageable = PageRequest.of(page, size)
        val result = issuedCouponJpaRepository.findAllByRefCouponId(couponId, pageable)
        return PageResult(result.content.map { it.toDomain() }, result.totalElements, page, size)
    }
}
