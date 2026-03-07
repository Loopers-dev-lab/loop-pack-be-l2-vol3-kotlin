package com.loopers.domain.coupon

import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult

interface CouponTemplateRepository {
    fun save(template: CouponTemplateModel): CouponTemplateModel
    fun findById(id: Long): CouponTemplateModel?
    fun findAll(pageQuery: PageQuery): PageResult<CouponTemplateModel>
}
