package com.loopers.domain.brand

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.ZonedDateTime

class FakeBrandRepository : BrandRepository {
    private val store = mutableListOf<BrandModel>()
    private var idSequence = 1L

    override fun save(brand: BrandModel): BrandModel {
        val idField = brand.javaClass.superclass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(brand, idSequence++)

        val now = ZonedDateTime.now()
        brand.javaClass.superclass.getDeclaredField("createdAt").apply {
            isAccessible = true
            set(brand, now)
        }
        brand.javaClass.superclass.getDeclaredField("updatedAt").apply {
            isAccessible = true
            set(brand, now)
        }

        store.add(brand)
        return brand
    }

    override fun findById(id: Long): BrandModel? {
        return store.find { it.id == id }
    }

    override fun findAll(pageable: Pageable): Page<BrandModel> {
        val start = pageable.offset.toInt()
        val end = minOf(start + pageable.pageSize, store.size)
        val content = if (start < store.size) store.subList(start, end) else emptyList()
        return PageImpl(content, pageable, store.size.toLong())
    }
}
