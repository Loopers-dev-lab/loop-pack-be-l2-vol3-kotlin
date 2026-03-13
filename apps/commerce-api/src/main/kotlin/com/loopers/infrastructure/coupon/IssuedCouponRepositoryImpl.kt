package com.loopers.infrastructure.coupon

import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import com.loopers.domain.coupon.IssuedCouponModel
import com.loopers.domain.coupon.IssuedCouponRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class IssuedCouponRepositoryImpl(
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
) : IssuedCouponRepository {
    override fun save(issuedCoupon: IssuedCouponModel): IssuedCouponModel {
        if (issuedCoupon.id == 0L) {
            return issuedCouponJpaRepository.save(IssuedCouponJpaModel.from(issuedCoupon)).toModel()
        }
        val existing = issuedCouponJpaRepository.findById(issuedCoupon.id).orElseThrow()
        existing.updateFrom(issuedCoupon)
        issuedCouponJpaRepository.flush()
        return existing.toModel()
    }

    override fun findById(id: Long): IssuedCouponModel? {
        return issuedCouponJpaRepository.findById(id).orElse(null)?.toModel()
    }

    override fun findAllByMemberId(memberId: Long): List<IssuedCouponModel> {
        return issuedCouponJpaRepository.findAllByMemberId(memberId).map { it.toModel() }
    }

    override fun findAllByTemplateId(templateId: Long, pageQuery: PageQuery): PageResult<IssuedCouponModel> {
        val pageable = PageRequest.of(pageQuery.page, pageQuery.size)
        val page = issuedCouponJpaRepository.findAllByCouponTemplateId(templateId, pageable)
        return PageResult(
            content = page.content.map { it.toModel() },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }
}
