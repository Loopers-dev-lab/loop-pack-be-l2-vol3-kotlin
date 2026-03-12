package com.loopers.infrastructure.product

import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductSearchCondition
import com.loopers.domain.product.ProductSortType
import com.loopers.domain.product.QProduct.product
import com.loopers.support.PageResult
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : ProductRepository {

    override fun findByIdOrNull(id: Long): Product? {
        return productJpaRepository.findByIdOrNull(id)
    }

    override fun findActiveByIdOrNull(id: Long): Product? {
        return productJpaRepository.findActiveByIdOrNull(id)
    }

    override fun findAllByCondition(condition: ProductSearchCondition): PageResult<Product> {
        val whereClause = buildWhere(condition)
        val orderBy = buildOrderBy(condition.sort)

        val content = queryFactory
            .selectFrom(product)
            .where(*whereClause.toTypedArray())
            .orderBy(*orderBy)
            .offset((condition.page * condition.size).toLong())
            .limit(condition.size.toLong())
            .fetch()

        val totalElements = queryFactory
            .select(product.count())
            .from(product)
            .where(*whereClause.toTypedArray())
            .fetchOne() ?: 0L

        return PageResult.of(
            content = content,
            page = condition.page,
            size = condition.size,
            totalElements = totalElements,
        )
    }

    override fun findAllActiveByBrandId(brandId: Long): List<Product> {
        return productJpaRepository.findAllActiveByBrandId(brandId)
    }

    override fun findAllActiveByIds(ids: List<Long>): List<Product> {
        if (ids.isEmpty()) return emptyList()
        return productJpaRepository.findAllActiveByIds(ids)
    }

    override fun findAllByIds(ids: List<Long>): List<Product> {
        if (ids.isEmpty()) return emptyList()
        return productJpaRepository.findAllByIds(ids)
    }

    override fun increaseLikeCount(id: Long) {
        productJpaRepository.increaseLikeCount(id)
    }

    override fun decreaseLikeCount(id: Long) {
        productJpaRepository.decreaseLikeCount(id)
    }

    override fun save(product: Product): Product {
        return productJpaRepository.save(product)
    }

    override fun saveAll(products: List<Product>): List<Product> {
        return productJpaRepository.saveAll(products)
    }

    private fun buildWhere(condition: ProductSearchCondition): List<BooleanExpression> {
        val predicates = mutableListOf<BooleanExpression>()

        if (!condition.includeDeleted) {
            predicates.add(product.deletedAt.isNull)
        }

        condition.brandId?.let {
            predicates.add(product.brandId.eq(it))
        }

        return predicates
    }

    private fun buildOrderBy(sortType: ProductSortType): Array<OrderSpecifier<*>> {
        val primary = when (sortType) {
            ProductSortType.LATEST -> product.createdAt.desc()
            ProductSortType.PRICE_ASC -> product.price.amount.asc()
            ProductSortType.LIKE_COUNT -> product.likeCount.desc()
        }
        return arrayOf(primary, product.id.desc())
    }
}
