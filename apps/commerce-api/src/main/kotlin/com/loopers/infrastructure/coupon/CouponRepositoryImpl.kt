package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponRepository
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.springframework.data.domain.PageRequest as SpringPageRequest
import org.springframework.stereotype.Repository

@Repository
class CouponRepositoryImpl(
    private val couponJpaRepository: CouponJpaRepository,
    private val couponMapper: CouponMapper,
) : CouponRepository {
    override fun save(coupon: Coupon): Coupon {
        val admin = "ADMIN"
        val entity = if (coupon.id != null) {
            val existing = couponJpaRepository.findById(coupon.id).orElseThrow()
            existing.name = coupon.name
            existing.discountValue = coupon.discountValue
            existing.minOrderAmount = coupon.minOrderAmount?.amount
            existing.expiredAt = coupon.expiredAt
            if (coupon.isDeleted()) existing.deleteBy(admin) else existing.updateBy(admin)
            existing
        } else {
            couponMapper.toEntity(coupon, admin)
        }
        return couponMapper.toDomain(couponJpaRepository.saveAndFlush(entity))
    }

    override fun findById(id: Long): Coupon? {
        return couponJpaRepository.findByIdAndDeletedAtIsNull(id)
            ?.let { couponMapper.toDomain(it) }
    }

    override fun findAll(pageRequest: PageRequest): PageResponse<Coupon> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = couponJpaRepository.findAllByDeletedAtIsNull(pageable)
        return PageResponse(
            content = page.content.map { couponMapper.toDomain(it) },
            totalElements = page.totalElements,
            page = pageRequest.page,
            size = pageRequest.size,
        )
    }
}
