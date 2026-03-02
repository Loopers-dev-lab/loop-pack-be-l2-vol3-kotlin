package com.loopers.domain.product

import com.loopers.domain.BaseEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class FakeProductRepository : ProductRepository {

    private val store = mutableListOf<Product>()
    private var idSequence = 1L

    override fun save(product: Product): Product {
        if (product.id == 0L) {
            setEntityId(product, idSequence++)
        }
        store.add(product)
        return product
    }

    override fun findById(id: Long): Product? {
        return store.find { it.id == id && it.deletedAt == null }
    }

    override fun findByIds(ids: List<Long>): List<Product> {
        return store.filter { it.id in ids && it.deletedAt == null }
    }

    override fun findByBrandId(brandId: Long): List<Product> {
        return store.filter { it.brandId == brandId && it.deletedAt == null }
    }

    override fun findAll(): List<Product> {
        return store.filter { it.deletedAt == null }
    }

    override fun findAllForUser(pageable: Pageable, brandId: Long?): Page<Product> {
        var filtered = store.filter {
            it.deletedAt == null &&
                it.status == ProductStatus.ACTIVE &&
                it.displayYn
        }
        if (brandId != null) {
            filtered = filtered.filter { it.brandId == brandId }
        }
        return toPage(filtered, pageable)
    }

    override fun findAllForAdmin(pageable: Pageable, brandId: Long?): Page<Product> {
        var filtered = store.filter { it.deletedAt == null }
        if (brandId != null) {
            filtered = filtered.filter { it.brandId == brandId }
        }
        return toPage(filtered, pageable)
    }

    override fun decreaseStock(productId: Long, quantity: Int): Int {
        val product = store.find { it.id == productId && it.deletedAt == null } ?: return 0
        if (product.stockQuantity < quantity) return 0
        product.decreaseStock(quantity)
        return 1
    }

    private fun toPage(list: List<Product>, pageable: Pageable): Page<Product> {
        val start = pageable.offset.toInt()
        val end = minOf(start + pageable.pageSize, list.size)
        val content = if (start <= list.size) list.subList(start, end) else emptyList()
        return PageImpl(content, pageable, list.size.toLong())
    }

    private fun setEntityId(entity: BaseEntity, id: Long) {
        val idField = BaseEntity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }
}
