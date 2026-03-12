package com.loopers.application.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductSortType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetProductListUseCase(
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val productCachePort: ProductCachePort,
) {
    fun getAllActive(brandId: Long?, sortType: ProductSortType): List<ProductInfo> {
        productCachePort.getProductList(brandId, sortType)?.let { return it }

        val products = productRepository.findAllActive(brandId, sortType)
        val result = toProductInfos(products)
        productCachePort.setProductList(brandId, sortType, result)
        return result
    }

    fun getAll(brandId: Long?): List<ProductInfo> {
        val products = productRepository.findAll(brandId)
        return toProductInfos(products)
    }

    private fun toProductInfos(products: List<Product>): List<ProductInfo> {
        val brandIds = products.map { it.refBrandId }.toSet()
        val brandMap: Map<Long, Brand> = brandRepository.findAllByIds(brandIds)
            .associateBy { requireNotNull(it.persistenceId) }
        return products.map { product ->
            val brand = requireNotNull(brandMap[product.refBrandId]) {
                "브랜드를 찾을 수 없습니다: ${product.refBrandId}"
            }
            ProductInfo.from(product, brand)
        }
    }
}
