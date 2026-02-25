package com.loopers.infrastructure.product

import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStatus
import org.springframework.stereotype.Component

@Component
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
    private val productMapper: ProductMapper,
) : ProductRepository {

    override fun save(product: Product): Product {
        val entity = resolveEntity(product)
        val savedEntity = productJpaRepository.save(entity)
        return productMapper.toDomain(savedEntity)
    }

    override fun findById(id: Long): Product? {
        return productJpaRepository.findById(id)
            .map { productMapper.toDomain(it) }
            .orElse(null)
    }

    override fun findAll(): List<Product> {
        return productJpaRepository.findAll().map { productMapper.toDomain(it) }
    }

    override fun findAllByBrandId(brandId: Long): List<Product> {
        return productJpaRepository.findAllByBrandId(brandId).map { productMapper.toDomain(it) }
    }

    override fun findAllByIds(ids: List<Long>): List<Product> {
        return productJpaRepository.findAllByIdIn(ids).map { productMapper.toDomain(it) }
    }

    override fun existsByBrandIdAndStatus(brandId: Long, status: ProductStatus): Boolean {
        return productJpaRepository.existsByBrandIdAndStatus(brandId, status.name)
    }

    private fun resolveEntity(product: Product): ProductEntity {
        if (product.id == null) return productMapper.toEntity(product)

        val entity = productJpaRepository.getReferenceById(product.id)
        productMapper.update(entity, product)
        return entity
    }
}
