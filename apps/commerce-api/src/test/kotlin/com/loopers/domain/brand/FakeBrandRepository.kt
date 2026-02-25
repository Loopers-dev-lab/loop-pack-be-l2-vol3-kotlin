package com.loopers.domain.brand

import com.loopers.domain.BaseEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class FakeBrandRepository : BrandRepository {

    private val store = mutableListOf<Brand>()
    private var idSequence = 1L

    override fun save(brand: Brand): Brand {
        if (brand.id == 0L) {
            setEntityId(brand, idSequence++)
        }
        store.add(brand)
        return brand
    }

    override fun findById(id: Long): Brand? {
        return store.find { it.id == id && it.deletedAt == null }
    }

    override fun findAll(): List<Brand> {
        return store.filter { it.deletedAt == null }
    }

    override fun findAll(pageable: Pageable): Page<Brand> {
        val filtered = store.filter { it.deletedAt == null }
        val start = pageable.offset.toInt()
        val end = minOf(start + pageable.pageSize, filtered.size)
        val content = if (start <= filtered.size) filtered.subList(start, end) else emptyList()
        return PageImpl(content, pageable, filtered.size.toLong())
    }

    private fun setEntityId(entity: BaseEntity, id: Long) {
        val idField = BaseEntity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }
}
