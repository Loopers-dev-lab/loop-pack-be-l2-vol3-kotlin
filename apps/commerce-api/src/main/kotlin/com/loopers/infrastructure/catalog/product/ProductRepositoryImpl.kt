package com.loopers.infrastructure.catalog.product

import com.loopers.domain.catalog.product.Product
import com.loopers.domain.catalog.product.ProductRepository
import com.loopers.domain.catalog.product.ProductSearchCondition
import com.loopers.domain.catalog.product.ProductSort
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
) : ProductRepository {

    override fun save(product: Product): Product {
        val entity = if (product.id > 0L) {
            productJpaRepository.getReferenceById(product.id).apply {
                update(product.name, product.description, product.price, product.stock)
                updateLikeCount(product.likeCount)
                updateStock(product.stock)
            }
        } else {
            ProductEntity.from(product)
        }
        return productJpaRepository.save(entity).toDomain()
    }

    override fun findById(id: Long): Product? =
        productJpaRepository.findById(id)
            .filter { it.deletedAt == null }
            .map { it.toDomain() }
            .orElse(null)

    override fun findAll(condition: ProductSearchCondition): List<Product> {
        val pageable = PageRequest.of(condition.page, condition.size)
        return when (condition.sort) {
            ProductSort.LATEST -> productJpaRepository.findAllActiveOrderByCreatedAtDesc(condition.brandId, pageable)
            ProductSort.PRICE_ASC -> productJpaRepository.findAllActiveOrderByPriceAsc(condition.brandId, pageable)
            ProductSort.LIKES_DESC -> productJpaRepository.findAllActiveOrderByLikeCountDesc(condition.brandId, pageable)
        }.map { it.toDomain() }
    }

    override fun findAllByBrandId(brandId: Long): List<Product> =
        productJpaRepository.findAllActiveByBrandId(brandId).map { it.toDomain() }

    override fun deleteById(id: Long) {
        productJpaRepository.findById(id)
            .filter { it.deletedAt == null }
            .ifPresent { it.delete() }
    }

    override fun deleteAllByBrandId(brandId: Long) {
        productJpaRepository.findAllActiveByBrandId(brandId).forEach { it.delete() }
    }
}
