package com.loopers.domain.coupon

import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult

interface IssuedCouponRepository {
    fun save(issuedCoupon: IssuedCouponModel): IssuedCouponModel
    fun findById(id: Long): IssuedCouponModel?
    fun findAllByMemberId(memberId: Long): List<IssuedCouponModel>
    fun findAllByTemplateId(templateId: Long, pageQuery: PageQuery): PageResult<IssuedCouponModel>
}
