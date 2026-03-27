package com.loopers.infrastructure.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductQueryRepository
import com.loopers.domain.product.ProductQueryResult
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStockRepository
import com.loopers.infrastructure.brand.QBrandEntity.brandEntity
import com.loopers.infrastructure.metric.ProductMetricJpaRepository
import com.loopers.infrastructure.metric.QProductMetricEntity.productMetricEntity
import com.loopers.infrastructure.product.QProductEntity.productEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class ProductQueryRepositoryImpl(
    private val redisProductQueryCache: RedisProductQueryCache,
    private val productRepository: ProductRepository,
    private val productStockRepository: ProductStockRepository,
    private val brandRepository: BrandRepository,
    private val productMetricJpaRepository: ProductMetricJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : ProductQueryRepository {

    @Transactional(readOnly = true)
    override fun getDetail(productId: Long): ProductQueryResult.Detail {
        redisProductQueryCache.getDetail(productId)?.let { cached ->
            return refreshDetailLikeCount(cached)
                .also(redisProductQueryCache::putDetail)
        }

        val product = productRepository.findById(productId)
            ?: throw CoreException(ErrorType.PRODUCT_NOT_FOUND)
        if (product.status != Product.Status.ACTIVE) {
            throw CoreException(ErrorType.PRODUCT_NOT_FOUND)
        }

        val brand = brandRepository.findById(product.brandId)
            ?: throw CoreException(ErrorType.BRAND_NOT_FOUND)
        if (brand.status != Brand.Status.ACTIVE) {
            throw CoreException(ErrorType.PRODUCT_NOT_FOUND)
        }

        val stock = productStockRepository.findByProductId(productId)
            ?: throw CoreException(ErrorType.PRODUCT_STOCK_NOT_FOUND)

        val likeCount = productLikeCount(productId)

        return ProductQueryResult.Detail.from(product, brand, stock)
            .copy(likeCount = likeCount)
            .also(redisProductQueryCache::putDetail)
    }

    @Transactional(readOnly = true)
    override fun getList(
        pageRequest: PageRequest,
        brandId: Long?,
        sort: Product.SortType?,
    ): PageResponse<ProductQueryResult.Summary> {
        return when (sort) {
            Product.SortType.LIKES_DESC -> findListByLikeCount(pageRequest, brandId)
            else -> {
                val namespaceVersion = redisProductQueryCache.getListNamespaceVersion(brandId)
                val cached = redisProductQueryCache.getList(pageRequest, brandId, sort, namespaceVersion)
                if (cached != null) {
                    val refreshed = refreshListLikeCounts(cached)
                    redisProductQueryCache.putList(pageRequest, brandId, sort, namespaceVersion, refreshed)
                    return refreshed
                }

                val result = findListByProductAggregate(pageRequest, brandId, sort)
                redisProductQueryCache.putList(pageRequest, brandId, sort, namespaceVersion, result)
                result
            }
        }
    }

    private fun findListByProductAggregate(
        pageRequest: PageRequest,
        brandId: Long?,
        sort: Product.SortType?,
    ): PageResponse<ProductQueryResult.Summary> {
        val productPage = productRepository.findAllActive(pageRequest, brandId, sort)
        val likeCounts = productLikeCounts(productPage.content.mapNotNull { it.id })
        val activeBrands = findActiveBrands(productPage.content.map { it.brandId }.distinct())

        return PageResponse(
            content = productPage.content.map { product ->
                val brand = activeBrands[product.brandId]
                    ?: throw CoreException(ErrorType.PRODUCT_NOT_FOUND)
                ProductQueryResult.Summary.from(product, brand)
                    .copy(likeCount = likeCounts[product.id] ?: 0)
            },
            totalElements = productPage.totalElements,
            page = productPage.page,
            size = productPage.size,
        )
    }

    private fun findListByLikeCount(
        pageRequest: PageRequest,
        brandId: Long?,
    ): PageResponse<ProductQueryResult.Summary> {
        val predicates = productQueryPredicates(brandId)
        val totalElements = queryFactory
            .select(productEntity.id.count())
            .from(productEntity)
            .join(brandEntity)
            .on(productEntity.brandId.eq(brandEntity.id))
            .leftJoin(productMetricEntity)
            .on(productEntity.id.eq(productMetricEntity.productId))
            .where(*predicates)
            .fetchOne() ?: 0L

        val content = queryFactory
            .select(
                Projections.constructor(
                    ProductQueryResult.Summary::class.java,
                    productEntity.id,
                    productEntity.name,
                    productEntity.sellingPrice,
                    productEntity.brandId,
                    brandEntity.name,
                    productEntity.thumbnailUrl,
                    productMetricEntity.likeCount.coalesce(0),
                ),
            )
            .from(productEntity)
            .join(brandEntity)
            .on(productEntity.brandId.eq(brandEntity.id))
            .leftJoin(productMetricEntity)
            .on(productEntity.id.eq(productMetricEntity.productId))
            .where(*predicates)
            .orderBy(
                productMetricEntity.likeCount.coalesce(0).desc(),
                productEntity.id.desc(),
            )
            .offset(pageRequest.page.toLong() * pageRequest.size)
            .limit(pageRequest.size.toLong())
            .fetch()

        return PageResponse(
            content = content,
            totalElements = totalElements,
            page = pageRequest.page,
            size = pageRequest.size,
        )
    }

    private fun productLikeCount(productId: Long): Int =
        productMetricJpaRepository.findByProductId(productId)?.likeCount ?: 0

    private fun refreshDetailLikeCount(detail: ProductQueryResult.Detail): ProductQueryResult.Detail =
        detail.copy(likeCount = productLikeCount(detail.id))

    private fun refreshListLikeCounts(
        response: PageResponse<ProductQueryResult.Summary>,
    ): PageResponse<ProductQueryResult.Summary> {
        val likeCounts = productLikeCounts(response.content.map { it.id })
        return response.map { summary ->
            summary.copy(likeCount = likeCounts[summary.id] ?: 0)
        }
    }

    private fun productLikeCounts(productIds: List<Long>): Map<Long, Int> =
        if (productIds.isEmpty()) {
            emptyMap()
        } else {
            productMetricJpaRepository.findAllByProductIdInAndDeletedAtIsNull(productIds)
                .associate { it.productId to it.likeCount }
        }

    private fun productQueryPredicates(brandId: Long?): Array<com.querydsl.core.types.Predicate> {
        val predicates = mutableListOf<com.querydsl.core.types.Predicate>(
            productEntity.status.eq(Product.Status.ACTIVE),
            productEntity.deletedAt.isNull,
            brandEntity.status.eq(Brand.Status.ACTIVE),
            brandEntity.deletedAt.isNull,
        )
        if (brandId != null) {
            predicates += brandEntity.id.eq(brandId)
        }
        return predicates.toTypedArray()
    }

    private fun findActiveBrands(brandIds: List<Long>): Map<Long, Brand> =
        brandRepository.findAllByIdIn(brandIds)
            .filter { it.status == Brand.Status.ACTIVE }
            .associateBy { it.id!! }
}
