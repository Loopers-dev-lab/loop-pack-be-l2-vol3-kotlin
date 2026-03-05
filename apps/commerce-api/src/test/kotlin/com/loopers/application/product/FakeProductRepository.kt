package com.loopers.application.product

import com.loopers.domain.common.CursorResult
import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductSearchCondition
import com.loopers.domain.product.ProductStatus
import java.time.ZonedDateTime

class FakeProductRepository : ProductRepository {
    private val store = mutableMapOf<Long, ProductModel>()
    private var idSequence = 1L

    override fun save(product: ProductModel): ProductModel {
        val now = ZonedDateTime.now()
        val saved = if (product.id == 0L) {
            product.copy(id = idSequence++, createdAt = now, updatedAt = now)
        } else {
            product.copy(updatedAt = now)
        }
        store[saved.id] = saved
        return saved
    }

    override fun findById(id: Long): ProductModel? {
        return store[id]
    }

    override fun findByIdWithLock(id: Long): ProductModel? {
        return store[id]
    }

    override fun findAll(pageQuery: PageQuery): PageResult<ProductModel> {
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

    override fun findAllByBrandId(brandId: Long, pageQuery: PageQuery): PageResult<ProductModel> {
        val filtered = store.values.filter { it.brandId == brandId }
        val start = pageQuery.page * pageQuery.size
        val end = minOf(start + pageQuery.size, filtered.size)
        val content = if (start < filtered.size) filtered.subList(start, end) else emptyList()
        return PageResult(
            content = content,
            totalElements = filtered.size.toLong(),
            totalPages = if (pageQuery.size > 0) (filtered.size + pageQuery.size - 1) / pageQuery.size else 0,
        )
    }

    override fun findAllByIdIn(ids: List<Long>): List<ProductModel> {
        return store.values.filter { it.id in ids }
    }

    override fun findActiveProducts(condition: ProductSearchCondition): CursorResult<ProductModel> {
        var result = store.values.filter { it.status == ProductStatus.ACTIVE }
        if (condition.brandId != null) {
            result = result.filter { it.brandId == condition.brandId }
        }
        val content = result.take(condition.size)
        return CursorResult(
            content = content,
            nextCursor = null,
            hasNext = result.size > condition.size,
        )
    }

    override fun findAllByBrandIdAndStatus(brandId: Long, status: ProductStatus): List<ProductModel> {
        return store.values.filter { it.brandId == brandId && it.status == status }
    }
}
