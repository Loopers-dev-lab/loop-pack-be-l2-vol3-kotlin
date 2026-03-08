package com.loopers.infrastructure.catalog

import com.loopers.domain.catalog.ProductModel
import com.loopers.domain.catalog.ProductRepository
import com.loopers.domain.catalog.ProductSortType
import com.loopers.domain.catalog.QProductModel.productModel
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.domain.SliceImpl
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : ProductRepository {
    override fun findById(id: Long): ProductModel? {
        return productJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findByIdWithLock(id: Long): ProductModel? {
        return productJpaRepository.findByIdWithLock(id)
    }

    override fun increaseLikeCount(id: Long): Boolean {
        return productJpaRepository.increaseLikeCount(id, ZonedDateTime.now()) > 0
    }

    override fun decreaseLikeCount(id: Long): Boolean {
        return productJpaRepository.decreaseLikeCount(id, ZonedDateTime.now()) > 0
    }

    override fun findAll(pageable: Pageable): Slice<ProductModel> {
        return productJpaRepository.findAllByDeletedAtIsNull(pageable)
    }

    override fun findAllByBrandId(brandId: Long): List<ProductModel> {
        return productJpaRepository.findAllByBrandIdAndDeletedAtIsNull(brandId)
    }

    override fun findAllByBrandId(brandId: Long, pageable: Pageable): Slice<ProductModel> {
        return productJpaRepository.findAllByBrandIdAndDeletedAtIsNull(brandId, pageable)
    }

    override fun search(sortType: ProductSortType, brandId: Long?, pageable: Pageable): Slice<ProductModel> {
        val query = queryFactory
            .selectFrom(productModel)
            .where(productModel.deletedAt.isNull)

        brandId?.let { query.where(productModel.brandId.eq(it)) }

        when (sortType) {
            ProductSortType.LATEST -> query.orderBy(productModel.id.desc())
            ProductSortType.PRICE_ASC -> query.orderBy(productModel.price.asc())
            ProductSortType.POPULAR -> query.orderBy(productModel.likeCount.desc(), productModel.id.desc())
        }

        val results = query
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong() + 1)
            .fetch()

        val hasNext = results.size > pageable.pageSize
        val content = if (hasNext) results.dropLast(1) else results
        return SliceImpl(content, pageable, hasNext)
    }

    override fun save(product: ProductModel): ProductModel {
        return productJpaRepository.save(product)
    }
}
