package com.loopers.domain.brand

import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult

interface BrandRepository {
    fun findById(id: Long): Brand?
    fun findAll(pageQuery: PageQuery): PageResult<Brand>
    fun findAllByIds(ids: List<Long>): List<Brand>
    fun save(brand: Brand): Brand
}
