package com.loopers.domain.fcfscoupon

import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult

interface FcfsCouponTemplateRepository {
    fun save(template: FcfsCouponTemplateModel): FcfsCouponTemplateModel
    fun findById(id: Long): FcfsCouponTemplateModel?
    fun findAll(pageQuery: PageQuery): PageResult<FcfsCouponTemplateModel>
}
