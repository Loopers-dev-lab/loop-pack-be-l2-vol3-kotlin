package com.loopers.domain.brand

import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult

interface BrandRepository {
    fun save(brand: BrandModel): BrandModel

    fun findById(id: Long): BrandModel?

    fun findAll(pageQuery: PageQuery): PageResult<BrandModel>
}
