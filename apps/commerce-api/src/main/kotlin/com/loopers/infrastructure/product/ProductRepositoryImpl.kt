package com.loopers.infrastructure.product

import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductSortType
import org.springframework.stereotype.Repository

@Repository
class ProductRepositoryImpl(
    private val jpaRepository: ProductJpaRepository,
) : ProductRepository {

    override fun save(product: Product): Long {
        val entity = ProductMapper.toEntity(product)
        val saved = jpaRepository.save(entity)
        return requireNotNull(saved.id) {
            "Product 저장 실패: id가 생성되지 않았습니다."
        }
    }

    override fun findById(id: Long): Product? {
        return jpaRepository.findByIdWithImages(id)?.let { ProductMapper.toDomain(it) }
    }

    override fun findByIdForUpdate(id: Long): Product? {
        return jpaRepository.findByIdForUpdate(id)?.let { ProductMapper.toDomain(it) }
    }

    override fun decreaseStock(id: Long, quantity: Int): Int {
        return jpaRepository.decreaseStock(id, quantity)
    }

    override fun increaseStock(id: Long, quantity: Int): Int {
        return jpaRepository.increaseStock(id, quantity)
    }

    override fun incrementLikeCount(id: Long): Int {
        return jpaRepository.incrementLikeCount(id)
    }

    override fun decrementLikeCount(id: Long): Int {
        return jpaRepository.decrementLikeCount(id)
    }

    override fun softDeleteByBrandId(brandId: Long): Int {
        return jpaRepository.softDeleteByBrandId(brandId)
    }

    override fun findAllActive(brandId: Long?, sortType: ProductSortType): List<Product> {
        val entities = when {
            brandId != null && sortType == ProductSortType.LIKE_COUNT ->
                jpaRepository.findAllActiveByBrandPopular(brandId)
            brandId != null ->
                jpaRepository.findAllActiveByBrandLatest(brandId)
            sortType == ProductSortType.LIKE_COUNT ->
                jpaRepository.findAllActivePopular()
            else ->
                jpaRepository.findAllActiveLatest()
        }
        return entities.map { ProductMapper.toDomain(it) }
    }

    override fun findAll(brandId: Long?): List<Product> {
        val entities = when {
            brandId != null -> jpaRepository.findAllByBrand(brandId)
            else -> jpaRepository.findAllLatest()
        }
        return entities.map { ProductMapper.toDomain(it) }
    }
}
