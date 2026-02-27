package com.loopers.infrastructure.product

import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductSort
import com.loopers.domain.product.QProduct.product
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : ProductRepository {
    override fun save(product: Product): Product {
        return productJpaRepository.save(product)
    }

    override fun findByIdAndDeletedAtIsNull(id: Long): Product? {
        return productJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findByIdWithPessimisticLock(id: Long): Product? {
        return productJpaRepository.findByIdWithPessimisticLock(id)
    }

    override fun findAllByCondition(brandId: Long?, sort: ProductSort, pageable: Pageable): Page<Product> {
        val query = queryFactory.selectFrom(product)
            .where(
                product.deletedAt.isNull,
                brandId?.let { product.brandId.eq(it) },
            )
            .orderBy(toOrderSpecifier(sort))
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())

        val content = query.fetch()

        val countQuery = queryFactory.select(product.count())
            .from(product)
            .where(
                product.deletedAt.isNull,
                brandId?.let { product.brandId.eq(it) },
            )

        val total = countQuery.fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }

    @Transactional
    override fun increaseLikeCount(productId: Long) {
        productJpaRepository.increaseLikeCount(productId)
    }

    @Transactional
    override fun decreaseLikeCount(productId: Long) {
        productJpaRepository.decreaseLikeCount(productId)
    }

    @Transactional
    override fun softDeleteByBrandId(brandId: Long) {
        productJpaRepository.softDeleteByBrandId(brandId)
    }

    private fun toOrderSpecifier(sort: ProductSort): OrderSpecifier<*> {
        return when (sort) {
            ProductSort.LATEST -> product.createdAt.desc()
            ProductSort.PRICE_ASC -> product.price.asc()
            ProductSort.LIKES_DESC -> product.likeCount.desc()
        }
    }
}
