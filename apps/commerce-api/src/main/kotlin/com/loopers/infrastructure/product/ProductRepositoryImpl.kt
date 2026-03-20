package com.loopers.infrastructure.product

import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductSortType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class ProductRepositoryImpl(private val productJpaRepository: ProductJpaRepository) : ProductRepository {

    override fun save(product: ProductModel): ProductModel = productJpaRepository.save(product)

    override fun findByIdAndDeletedAtIsNull(id: Long): ProductModel? = productJpaRepository.findByIdAndDeletedAtIsNull(id)

    override fun findAllByDeletedAtIsNull(
        brandId: Long?,
        sortType: ProductSortType,
        pageable: Pageable,
    ): Page<ProductModel> = when {
        brandId == null && sortType == ProductSortType.LATEST ->
            productJpaRepository.findAllByDeletedAtIsNullOrderByCreatedAtDescIdDesc(pageable)

        brandId == null && sortType == ProductSortType.PRICE_ASC ->
            productJpaRepository.findAllByDeletedAtIsNullOrderByPriceAscIdDesc(pageable)

        brandId == null && sortType == ProductSortType.LIKES_DESC ->
            productJpaRepository.findAllByDeletedAtIsNullOrderByLikesCountDescIdDesc(pageable)

        sortType == ProductSortType.LATEST ->
            productJpaRepository.findAllByBrandIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(brandId!!, pageable)

        sortType == ProductSortType.PRICE_ASC ->
            productJpaRepository.findAllByBrandIdAndDeletedAtIsNullOrderByPriceAscIdDesc(brandId!!, pageable)

        else ->
            productJpaRepository.findAllByBrandIdAndDeletedAtIsNullOrderByLikesCountDescIdDesc(brandId!!, pageable)
    }

    override fun findAllByIdInAndDeletedAtIsNull(ids: List<Long>): List<ProductModel> {
        return productJpaRepository.findAllByIdInAndDeletedAtIsNull(ids)
    }

    override fun findAllByIdsForUpdate(ids: List<Long>): List<ProductModel> {
        return productJpaRepository.findAllByIdsForUpdate(ids)
    }
}
