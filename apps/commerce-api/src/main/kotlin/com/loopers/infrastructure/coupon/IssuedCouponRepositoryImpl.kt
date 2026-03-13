package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.coupon.IssuedCouponRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.springframework.data.domain.PageRequest as SpringPageRequest
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Repository
class IssuedCouponRepositoryImpl(
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
    private val issuedCouponMapper: IssuedCouponMapper,
) : IssuedCouponRepository {
    override fun save(issuedCoupon: IssuedCoupon): IssuedCoupon {
        return issuedCouponMapper.toDomain(
            issuedCouponJpaRepository.saveAndFlush(issuedCouponMapper.toEntity(issuedCoupon)),
        )
    }

    override fun use(issuedCoupon: IssuedCoupon) {
        val now = ZonedDateTime.now()
        val updatedRows = issuedCouponJpaRepository.useOptimistic(
            id = issuedCoupon.id!!,
            version = issuedCoupon.version,
            usedAt = issuedCoupon.usedAt ?: now,
            now = now,
        )
        if (updatedRows == 0) {
            throw CoreException(ErrorType.ISSUED_COUPON_CONFLICT)
        }
    }

    override fun findById(id: Long): IssuedCoupon? {
        return issuedCouponJpaRepository.findByIdAndDeletedAtIsNull(id)
            ?.let { issuedCouponMapper.toDomain(it) }
    }

    override fun findAllByUserId(userId: Long): List<IssuedCoupon> {
        return issuedCouponJpaRepository.findAllByUserIdAndDeletedAtIsNull(userId)
            .map { issuedCouponMapper.toDomain(it) }
    }

    override fun findAllByCouponId(
        couponId: Long,
        pageRequest: PageRequest,
    ): PageResponse<IssuedCoupon> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = issuedCouponJpaRepository.findAllByCouponIdAndDeletedAtIsNull(couponId, pageable)
        return PageResponse(
            content = page.content.map { issuedCouponMapper.toDomain(it) },
            totalElements = page.totalElements,
            page = pageRequest.page,
            size = pageRequest.size,
        )
    }
}
