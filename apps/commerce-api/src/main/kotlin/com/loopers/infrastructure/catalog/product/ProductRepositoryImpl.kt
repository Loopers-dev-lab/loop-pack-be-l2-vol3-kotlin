package com.loopers.infrastructure.catalog.product

import com.loopers.domain.PageResult
import com.loopers.domain.catalog.product.entity.Product
import com.loopers.domain.catalog.product.ProductSort
import com.loopers.domain.catalog.product.repository.ProductRepository
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface ProductJpaRepository : JpaRepository<ProductEntity, Long> {
    fun findAllByRefBrandId(brandId: Long): List<ProductEntity>
}

@Repository
class ProductRepositoryImpl(
    private val jpa: ProductJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : ProductRepository {

    override fun save(product: Product): Product {
        return jpa.save(ProductEntity.fromDomain(product)).toDomain()
    }

    override fun findById(id: Long): Product? {
        return jpa.findById(id).orElse(null)?.toDomain()
    }

    override fun findAll(page: Int, size: Int): PageResult<Product> {
        val pageable = PageRequest.of(page, size)
        val result = jpa.findAll(pageable)
        return PageResult(result.content.map { it.toDomain() }, result.totalElements, page, size)
    }

    override fun findActiveProducts(brandId: Long?, sort: ProductSort, page: Int, size: Int): PageResult<Product> {
        val product = QProductEntity.productEntity

        val where = BooleanBuilder()
            .and(product.deletedAt.isNull)
            .and(product.status.ne(Product.ProductStatus.HIDDEN))
        brandId?.let { where.and(product.refBrandId.eq(it)) }

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

    override fun findAllByBrandId(brandId: Long): List<Product> {
        return jpa.findAllByRefBrandId(brandId).map { it.toDomain() }
    }

    override fun findAllByIds(ids: List<Long>): List<Product> {
        return jpa.findAllById(ids).map { it.toDomain() }
    }
}
