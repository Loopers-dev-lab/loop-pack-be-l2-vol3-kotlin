package com.loopers.application.product

import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductSearchCondition
import com.loopers.domain.product.ProductStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.ZonedDateTime

class FakeProductRepository : ProductRepository {
    private val store = mutableListOf<ProductModel>()
    private var idSequence = 1L

    override fun save(product: ProductModel): ProductModel {
        val idField = product.javaClass.superclass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(product, idSequence++)

        val now = ZonedDateTime.now()
        product.javaClass.superclass.getDeclaredField("createdAt").apply {
            isAccessible = true
            set(product, now)
        }
        product.javaClass.superclass.getDeclaredField("updatedAt").apply {
            isAccessible = true
            set(product, now)
        }

        store.add(product)
        return product
    }

    override fun findById(id: Long): ProductModel? {
        return store.find { it.id == id }
    }

    override fun findByIdWithLock(id: Long): ProductModel? {
        return store.find { it.id == id }
    }

    override fun findAll(pageable: Pageable): Page<ProductModel> {
        val start = pageable.offset.toInt()
        val end = minOf(start + pageable.pageSize, store.size)
        val content = if (start < store.size) store.subList(start, end) else emptyList()
        return PageImpl(content, pageable, store.size.toLong())
    }

    override fun findAllByBrandId(brandId: Long, pageable: Pageable): Page<ProductModel> {
        val filtered = store.filter { it.brandId == brandId }
        val start = pageable.offset.toInt()
        val end = minOf(start + pageable.pageSize, filtered.size)
        val content = if (start < filtered.size) filtered.subList(start, end) else emptyList()
        return PageImpl(content, pageable, filtered.size.toLong())
    }

    override fun findAllByIdIn(ids: List<Long>): List<ProductModel> {
        return store.filter { it.id in ids }
    }

    override fun findActiveProducts(condition: ProductSearchCondition): List<ProductModel> {
        var result = store.filter { it.status == ProductStatus.ACTIVE }
        if (condition.brandId != null) {
            result = result.filter { it.brandId == condition.brandId }
        }
        return result.take(condition.size)
    }

    override fun findAllByBrandIdAndStatus(brandId: Long, status: ProductStatus): List<ProductModel> {
        return store.filter { it.brandId == brandId && it.status == status }
    }
}
