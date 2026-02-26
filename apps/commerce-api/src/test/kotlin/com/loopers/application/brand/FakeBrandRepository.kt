package com.loopers.application.brand

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import java.time.ZonedDateTime

class FakeBrandRepository : BrandRepository {
    private val store = mutableMapOf<Long, BrandModel>()
    private var idSequence = 1L

    override fun save(brand: BrandModel): BrandModel {
        val now = ZonedDateTime.now()
        val saved = if (brand.id == 0L) {
            brand.copy(id = idSequence++, createdAt = now, updatedAt = now)
        } else {
            brand.copy(updatedAt = now)
        }
        store[saved.id] = saved
        return saved
    }

    override fun findById(id: Long): BrandModel? {
        return store[id]
    }

    override fun findAll(pageQuery: PageQuery): PageResult<BrandModel> {
        val all = store.values.toList()
        val start = pageQuery.page * pageQuery.size
        val end = minOf(start + pageQuery.size, all.size)
        val content = if (start < all.size) all.subList(start, end) else emptyList()
        return PageResult(
            content = content,
            totalElements = all.size.toLong(),
            totalPages = if (pageQuery.size > 0) (all.size + pageQuery.size - 1) / pageQuery.size else 0,
        )
    }
}
