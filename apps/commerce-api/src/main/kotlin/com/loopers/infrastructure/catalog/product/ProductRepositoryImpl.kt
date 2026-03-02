package com.loopers.infrastructure.catalog.product

import com.loopers.domain.PageResult
import com.loopers.domain.catalog.product.ProductSort
import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.common.vo.BrandId
import com.loopers.domain.common.vo.ProductId
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.stereotype.Repository

interface ProductJpaRepository : JpaRepository<ProductEntity, Long> {
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<ProductEntity>
    fun findAllByRefBrandIdAndDeletedAtIsNull(brandId: Long): List<ProductEntity>
    fun findAllByIdInAndDeletedAtIsNull(ids: List<Long>): List<ProductEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findForUpdateById(id: Long): ProductEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findAllForUpdateByIdInAndDeletedAtIsNullOrderByIdAsc(ids: List<Long>): List<ProductEntity>
}

@Repository
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : ProductRepository {

    override fun save(product: Product): Product {
        return productJpaRepository.save(ProductEntity.fromDomain(product)).toDomain()
    }

    override fun saveAll(products: List<Product>): List<Product> {
        return productJpaRepository.saveAll(products.map { ProductEntity.fromDomain(it) }).map { it.toDomain() }
    }

    override fun findById(id: ProductId): Product? {
        return productJpaRepository.findById(id.value).orElse(null)?.toDomain()
    }

    override fun findByIdForUpdate(id: ProductId): Product? {
        return productJpaRepository.findForUpdateById(id.value)?.toDomain()
    }

    override fun findAll(page: Int, size: Int): PageResult<Product> {
        val pageable = PageRequest.of(page, size)
        val result = productJpaRepository.findAllByDeletedAtIsNull(pageable)
        return PageResult(result.content.map { it.toDomain() }, result.totalElements, page, size)
    }

    override fun findAllIncludeDeleted(page: Int, size: Int): PageResult<Product> {
        val pageable = PageRequest.of(page, size)
        val result = productJpaRepository.findAll(pageable)
        return PageResult(result.content.map { it.toDomain() }, result.totalElements, page, size)
    }

    override fun findActiveProducts(brandId: BrandId?, sort: ProductSort, page: Int, size: Int): PageResult<Product> {
        val product = QProductEntity.productEntity

        val where = BooleanBuilder()
            .and(product.deletedAt.isNull)
            .and(product.status.ne(Product.ProductStatus.HIDDEN))
        brandId?.let { where.and(product.refBrandId.eq(it.value)) }

        val orderSpecifier = when (sort) {
            ProductSort.LATEST -> product.createdAt.desc()
            ProductSort.PRICE_ASC -> product.price.asc()
            ProductSort.LIKES_DESC -> product.likeCount.desc()
        }

        val offset = page.toLong() * size

        val content = queryFactory.selectFrom(product)
            .where(where)
            .orderBy(orderSpecifier)
            .offset(offset)
            .limit(size.toLong())
            .fetch()

        val total = queryFactory.select(product.count())
            .from(product)
            .where(where)
            .fetchOne() ?: 0L

        return PageResult(content.map { it.toDomain() }, total, page, size)
    }

    override fun findAllByBrandId(brandId: BrandId): List<Product> {
        return productJpaRepository.findAllByRefBrandIdAndDeletedAtIsNull(brandId.value).map { it.toDomain() }
    }

    override fun findAllByIds(ids: List<ProductId>): List<Product> {
        return productJpaRepository.findAllByIdInAndDeletedAtIsNull(ids.map { it.value }).map { it.toDomain() }
    }

    override fun findAllByIdsForUpdate(ids: List<ProductId>): List<Product> {
        val sortedIds = ids.map { it.value }.distinct().sorted()
        return productJpaRepository.findAllForUpdateByIdInAndDeletedAtIsNullOrderByIdAsc(sortedIds).map { it.toDomain() }
    }
}
