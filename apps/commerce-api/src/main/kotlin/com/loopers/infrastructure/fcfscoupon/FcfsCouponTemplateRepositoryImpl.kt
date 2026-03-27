package com.loopers.infrastructure.fcfscoupon

import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import com.loopers.domain.fcfscoupon.FcfsCouponTemplateModel
import com.loopers.domain.fcfscoupon.FcfsCouponTemplateRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class FcfsCouponTemplateRepositoryImpl(
    private val jpaRepository: FcfsCouponTemplateJpaRepository,
) : FcfsCouponTemplateRepository {

    override fun save(template: FcfsCouponTemplateModel): FcfsCouponTemplateModel {
        if (template.id == 0L) {
            return jpaRepository.save(FcfsCouponTemplateJpaModel.from(template)).toModel()
        }
        val entity = jpaRepository.findById(template.id).orElseThrow()
        entity.updateFrom(template)
        return jpaRepository.save(entity).toModel()
    }

    override fun findById(id: Long): FcfsCouponTemplateModel? {
        return jpaRepository.findById(id).orElse(null)?.toModel()
    }

    override fun findAll(pageQuery: PageQuery): PageResult<FcfsCouponTemplateModel> {
        val pageable = PageRequest.of(pageQuery.page, pageQuery.size)
        val page = jpaRepository.findAllByOrderByIdDesc(pageable)
        return PageResult(
            content = page.content.map { it.toModel() },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }
}
