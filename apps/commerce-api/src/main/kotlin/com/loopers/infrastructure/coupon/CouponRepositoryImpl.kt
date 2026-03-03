package com.loopers.infrastructure.coupon

import com.loopers.domain.PageResult
import com.loopers.domain.coupon.model.Coupon
import com.loopers.domain.coupon.repository.CouponRepository
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.stereotype.Repository

interface CouponJpaRepository : JpaRepository<CouponEntity, Long> {
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<CouponEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithLockById(id: Long): CouponEntity?
}

@Repository
class CouponRepositoryImpl(
    private val couponJpaRepository: CouponJpaRepository,
) : CouponRepository {

    override fun save(coupon: Coupon): Coupon =
        couponJpaRepository.save(CouponEntity.fromDomain(coupon)).toDomain()

    override fun findById(id: Long): Coupon? =
        couponJpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByIdForUpdate(id: Long): Coupon? =
        couponJpaRepository.findWithLockById(id)?.toDomain()

    override fun findAll(page: Int, size: Int): PageResult<Coupon> {
        val pageable = PageRequest.of(page, size)
        val result = couponJpaRepository.findAllByDeletedAtIsNull(pageable)
        return PageResult(result.content.map { it.toDomain() }, result.totalElements, page, size)
    }

    override fun findAllIncludeDeleted(page: Int, size: Int): PageResult<Coupon> {
        val pageable = PageRequest.of(page, size)
        val result = couponJpaRepository.findAll(pageable)
        return PageResult(result.content.map { it.toDomain() }, result.totalElements, page, size)
    }
}
