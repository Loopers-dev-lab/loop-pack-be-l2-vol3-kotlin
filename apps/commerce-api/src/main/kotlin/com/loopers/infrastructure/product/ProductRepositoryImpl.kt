package com.loopers.infrastructure.product

import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStatus
import com.loopers.domain.product.QProduct
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Order
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : ProductRepository {

    override fun findById(id: Long): Product? =
        productJpaRepository
            .findByIdOrNull(id)

    override fun findProductWithLock(id: Long): Product? =
        productJpaRepository.findByIdWithLock(id)

    override fun findByBrandId(brandId: Long): List<Product> =
        productJpaRepository.findByBrandId(brandId)

    override fun findWithPaging(brandId: Long?, pageable: Pageable): Page<Product> {
        val qProduct = QProduct.product

        val predicate = BooleanBuilder()
        if (brandId != null) {
            predicate.and(qProduct.brand.id.eq(brandId))
        }
        predicate.and(qProduct.deletedAt.isNull())

        val orders = pageable.sort.mapNotNull { order ->
            val path = when (order.property) {
                "createdAt" -> qProduct.createdAt
                "price" -> qProduct.price
                else -> null
            }
            path?.let {
                val direction = if (order.isAscending) Order.ASC else Order.DESC
                OrderSpecifier(direction, it)
            }
        }

        val content = queryFactory
            .selectFrom(qProduct)
            .innerJoin(qProduct.brand).fetchJoin()
            .where(predicate)
            .apply {
                if (orders.isNotEmpty()) {
                    orderBy(*orders.toTypedArray())
                }
            }
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = productJpaRepository.count(predicate)

        return PageImpl(content, pageable, total)
    }

    override fun findActiveProductsWithPaging(brandId: Long?, pageable: Pageable): Page<Product> {
        val qProduct = QProduct.product

        val predicate = BooleanBuilder()
        if (brandId != null) {
            predicate.and(qProduct.brand.id.eq(brandId))
        }
        predicate.and(qProduct.deletedAt.isNull())
        predicate.and(qProduct.status.eq(ProductStatus.ACTIVE))

        val orders = pageable.sort.mapNotNull { order ->
            val path = when (order.property) {
                "createdAt" -> qProduct.createdAt
                "price" -> qProduct.price
                else -> null
            }
            path?.let {
                val direction = if (order.isAscending) Order.ASC else Order.DESC
                OrderSpecifier(direction, it)
            }
        }

        val content = queryFactory
            .selectFrom(qProduct)
            .innerJoin(qProduct.brand).fetchJoin()
            .where(predicate)
            .apply {
                if (orders.isNotEmpty()) {
                    orderBy(*orders.toTypedArray())
                }
            }
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = productJpaRepository.count(predicate)

        return PageImpl(content, pageable, total)
    }

    override fun save(product: Product): Product = productJpaRepository.save(product)
}
