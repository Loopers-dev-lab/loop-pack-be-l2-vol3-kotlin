package com.loopers.infrastructure.catalog.product

import com.loopers.domain.PageResult
import com.loopers.domain.catalog.product.entity.Product
import com.loopers.domain.catalog.product.ProductSort
import com.loopers.domain.catalog.product.entity.QProduct
import com.loopers.domain.catalog.product.repository.ProductRepository
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : ProductRepository {

    override fun save(product: Product): Product {
        return productJpaRepository.save(product)
    }

    override fun findById(id: Long): Product? {
        return productJpaRepository.findById(id).orElse(null)
    }

    override fun findAll(page: Int, size: Int): PageResult<Product> {
        val pageable = PageRequest.of(page, size)
        val result = productJpaRepository.findAll(pageable)
        return PageResult(result.content, result.totalElements, page, size)
    }

    override fun findActiveProducts(brandId: Long?, sort: ProductSort, page: Int, size: Int): PageResult<Product> {
        val product = QProduct.product

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

        return PageResult(content, total, page, size)
    }

    override fun findAllByBrandId(brandId: Long): List<Product> {
        return productJpaRepository.findAllByRefBrandId(brandId)
    }

    override fun findAllByIds(ids: List<Long>): List<Product> {
        return productJpaRepository.findAllById(ids)
    }
}
