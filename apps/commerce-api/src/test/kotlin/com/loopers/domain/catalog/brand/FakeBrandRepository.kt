package com.loopers.domain.catalog.brand

import com.loopers.domain.PageResult
import com.loopers.domain.catalog.brand.model.Brand
import com.loopers.domain.catalog.brand.repository.BrandRepository

class FakeBrandRepository : BrandRepository {

    private val brands = mutableListOf<Brand>()
    private var sequence = 1L

    override fun save(brand: Brand): Brand {
        if (brand.id != 0L) {
            brands.removeIf { it.id == brand.id }
            brands.add(brand)
            return brand
        }
        val saved = Brand(
            id = sequence++,
            name = brand.name,
            deletedAt = brand.deletedAt,
        )
        brands.add(saved)
        return saved
    }

    override fun findById(id: Long): Brand? {
        return brands.find { it.id == id }
    }

    override fun findAll(page: Int, size: Int): PageResult<Brand> {
        val offset = page * size
        val active = brands.filter { it.deletedAt == null }
        val content = active.drop(offset).take(size)
        return PageResult(content, active.size.toLong(), page, size)
    }
}
