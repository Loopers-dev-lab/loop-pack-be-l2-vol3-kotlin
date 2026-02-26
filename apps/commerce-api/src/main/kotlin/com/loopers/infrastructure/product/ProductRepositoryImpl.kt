package com.loopers.infrastructure.product

import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
) : ProductRepository {

    override fun save(product: Product): Product {
        return productJpaRepository.save(product)
    }

    override fun findById(id: Long): Product? {
        return productJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findByIdIncludingDeleted(id: Long): Product? {
        return productJpaRepository.findById(id).orElse(null)
    }

    override fun findByIdWithLock(id: Long): Product? {
        return productJpaRepository.findByIdWithLock(id)
    }

    override fun findAll(pageable: Pageable): Page<Product> {
        return productJpaRepository.findAllByDeletedAtIsNull(pageable)
    }

    override fun findAllByBrandId(brandId: Long, pageable: Pageable): Page<Product> {
        return productJpaRepository.findAllByBrandIdAndDeletedAtIsNull(brandId, pageable)
    }

    override fun findAllByBrandId(brandId: Long): List<Product> {
        return productJpaRepository.findAllByBrandIdAndDeletedAtIsNull(brandId)
    }

    override fun findAllByIdWithLock(ids: List<Long>): List<Product> {
        return productJpaRepository.findAllByIdWithLock(ids)
    }
}
