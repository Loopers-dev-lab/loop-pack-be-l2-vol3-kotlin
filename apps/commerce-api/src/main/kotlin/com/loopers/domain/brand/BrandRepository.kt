package com.loopers.domain.brand

import com.loopers.support.common.PageQuery
import com.loopers.support.common.PageResult

interface BrandRepository {
    fun findById(id: Long): Brand?
    fun findAll(pageQuery: PageQuery): PageResult<Brand>
    fun findAllByIds(ids: List<Long>): List<Brand>
    fun save(brand: Brand): Brand
}
