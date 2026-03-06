package com.loopers.infrastructure.productlike

import com.loopers.domain.product.Product
import com.loopers.domain.product.QProduct
import com.loopers.domain.productlike.ProductLike
import com.loopers.domain.productlike.ProductLikeRepository
import com.loopers.domain.productlike.QProductLike
import com.querydsl.core.types.Order
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class ProductLikeRepositoryImpl(
    private val productLikeJpaRepository: ProductLikeJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : ProductLikeRepository {

    override fun findByUserIdAndProductId(userId: Long, productId: Long): ProductLike? =
        productLikeJpaRepository.findByUserIdAndProductId(userId, productId)

    override fun findLikedProducts(userId: Long, pageable: Pageable): Page<Product> {
        val qProductLike = QProductLike.productLike
        val qProduct = QProduct.product

        val content = queryFactory
            .selectFrom(qProduct)
            .innerJoin(qProductLike).on(qProductLike.product.id.eq(qProduct.id))
            .where(qProductLike.user.id.eq(userId))
            .innerJoin(qProduct.brand).fetchJoin()
            .orderBy(OrderSpecifier(Order.DESC, qProductLike.createdAt))
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(qProduct.countDistinct())
            .from(qProduct)
            .innerJoin(qProductLike).on(qProductLike.product.id.eq(qProduct.id))
            .where(qProductLike.user.id.eq(userId))
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }

    override fun save(productLike: ProductLike): ProductLike =
        productLikeJpaRepository.save(productLike)

    override fun delete(productLike: ProductLike) {
        productLikeJpaRepository.delete(productLike)
    }
}
