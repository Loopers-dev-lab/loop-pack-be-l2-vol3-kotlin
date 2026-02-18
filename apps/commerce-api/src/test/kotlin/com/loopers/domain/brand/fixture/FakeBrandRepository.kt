package com.loopers.domain.brand.fixture

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandName
import com.loopers.domain.brand.BrandRepository

class FakeBrandRepository : BrandRepository {

    private val store = HashMap<Long, Brand>()
    private var sequence = 1L

    override fun save(brand: Brand): Long {
        val id = brand.persistenceId ?: sequence++
        val persisted = Brand.reconstitute(
            persistenceId = id,
            name = brand.name,
            description = brand.description,
            logoUrl = brand.logoUrl,
            status = brand.status,
            deletedAt = brand.deletedAt,
        )
        store[id] = persisted
        return id
    }

    override fun findById(id: Long): Brand? {
        return store[id]
    }

    override fun existsByName(name: BrandName): Boolean {
        return store.values.any { it.name == name }
    }

    override fun findAll(): List<Brand> {
        return store.values.toList()
    }

    override fun findAllActive(): List<Brand> {
        return store.values.filter { !it.isDeleted() }
    }

    override fun findAllByIds(ids: Set<Long>): List<Brand> {
        return ids.mapNotNull { store[it] }
    }
}
