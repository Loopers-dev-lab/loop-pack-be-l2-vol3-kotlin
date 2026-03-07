package com.loopers.infrastructure.coupon

import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import com.loopers.domain.coupon.CouponTemplateModel
import com.loopers.domain.coupon.CouponTemplateRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class CouponTemplateRepositoryImpl(
    private val couponTemplateJpaRepository: CouponTemplateJpaRepository,
) : CouponTemplateRepository {
    override fun save(template: CouponTemplateModel): CouponTemplateModel {
        if (template.id == 0L) {
            return couponTemplateJpaRepository.save(CouponTemplateJpaModel.from(template)).toModel()
        }
        val existing = couponTemplateJpaRepository.findById(template.id).orElseThrow()
        existing.updateFrom(template)
        return existing.toModel()
    }

    override fun findById(id: Long): CouponTemplateModel? {
        return couponTemplateJpaRepository.findById(id).orElse(null)?.toModel()
    }

    override fun findAll(pageQuery: PageQuery): PageResult<CouponTemplateModel> {
        val pageable = PageRequest.of(pageQuery.page, pageQuery.size)
        val page = couponTemplateJpaRepository.findAllByOrderByIdDesc(pageable)
        return PageResult(
            content = page.content.map { it.toModel() },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }
}
