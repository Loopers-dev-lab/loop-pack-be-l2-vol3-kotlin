package com.loopers.infrastructure.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductQueryRepository
import com.loopers.domain.product.ProductQueryResult
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStockRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class ProductQueryRepositoryImpl(
    private val redisProductQueryCache: RedisProductQueryCache,
    private val productRepository: ProductRepository,
    private val productStockRepository: ProductStockRepository,
    private val brandRepository: BrandRepository,
) : ProductQueryRepository {

    @Transactional(readOnly = true)
    override fun getDetail(productId: Long): ProductQueryResult.Detail {
        redisProductQueryCache.getDetail(productId)?.let { return it }

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

        return ProductQueryResult.Detail.from(product, brand, stock)
            .also(redisProductQueryCache::putDetail)
    }

    @Transactional(readOnly = true)
    override fun getList(
        pageRequest: PageRequest,
        brandId: Long?,
        sort: Product.SortType?,
    ): PageResponse<ProductQueryResult.Summary> {
        val namespaceVersion = redisProductQueryCache.getListNamespaceVersion(brandId)
        redisProductQueryCache.getList(pageRequest, brandId, sort, namespaceVersion)?.let { return it }

        val productPage = productRepository.findAllActive(pageRequest, brandId, sort)
        val activeBrands = findActiveBrands(productPage.content.map { it.brandId }.distinct())

        return PageResponse(
            content = productPage.content.map { product ->
                val brand = activeBrands[product.brandId]
                    ?: throw CoreException(ErrorType.PRODUCT_NOT_FOUND)
                ProductQueryResult.Summary.from(product, brand)
            },
            totalElements = productPage.totalElements,
            page = productPage.page,
            size = productPage.size,
        ).also {
            redisProductQueryCache.putList(pageRequest, brandId, sort, namespaceVersion, it)
        }
    }

    private fun findActiveBrands(brandIds: List<Long>): Map<Long, Brand> =
        brandRepository.findAllByIdIn(brandIds)
            .filter { it.status == Brand.Status.ACTIVE }
            .associateBy { it.id!! }
}
