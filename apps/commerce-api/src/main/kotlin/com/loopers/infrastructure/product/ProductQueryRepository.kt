package com.loopers.infrastructure.product

import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductSortType
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Component

@Component
class ProductQueryRepository(
    private val queryFactory: JPAQueryFactory,
    private val productMapper: ProductMapper,
) {
    private val product = QProductEntity.productEntity

    fun findAll(sortType: ProductSortType, brandId: Long?): List<Product> {
        return queryFactory.selectFrom(product)
            .where(brandIdEq(brandId))
            .orderBy(*orderSpecifiers(sortType))
            .fetch()
            .map(productMapper::toDomain)
    }

    private fun brandIdEq(brandId: Long?): BooleanExpression? {
        return brandId?.let { product.brandId.eq(it) }
    }

    private fun orderSpecifiers(sortType: ProductSortType): Array<OrderSpecifier<*>> {
        return when (sortType) {
            ProductSortType.LATEST -> arrayOf(product.id.desc())
            ProductSortType.PRICE_ASC -> arrayOf(product.price.asc(), product.id.desc())
            ProductSortType.LIKES_DESC -> arrayOf(product.likeCount.desc(), product.id.desc())
        }
    }
}
