package com.loopers.domain.catalog.brand

import com.loopers.domain.BaseEntity
import com.loopers.domain.PageResult
import com.loopers.domain.catalog.brand.entity.Brand
import com.loopers.domain.catalog.brand.repository.BrandRepository

class FakeBrandRepository : BrandRepository {

    private val brands = mutableListOf<Brand>()
    private var sequence = 1L

    override fun save(brand: Brand): Brand {
        if (brand.id != 0L) {
            brands.removeIf { it.id == brand.id }
            brands.add(brand)
        } else {
            setEntityId(brand, sequence++)
            brands.add(brand)
        }
        return brand
    }

    override fun findById(id: Long): Brand? {
        return brands.find { it.id == id }
    }

    override fun findAll(page: Int, size: Int): PageResult<Brand> {
        val offset = page * size
        val content = brands.drop(offset).take(size)
        return PageResult(content, brands.size.toLong(), page, size)
    }

    private fun setEntityId(entity: BaseEntity, id: Long) {
        BaseEntity::class.java.getDeclaredField("id").apply {
            isAccessible = true
            set(entity, id)
        }
    }
}
