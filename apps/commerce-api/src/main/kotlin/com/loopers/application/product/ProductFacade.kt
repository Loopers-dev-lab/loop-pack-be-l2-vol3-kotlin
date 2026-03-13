package com.loopers.application.product

import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductSortType
import com.loopers.domain.product.ProductService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ProductFacade(
    private val productService: ProductService,
    private val brandService: BrandService,
    private val productCacheStore: ProductCacheStore,
) {
    @Transactional(readOnly = true)
    fun getProductDetail(productId: Long): ProductInfo {
        productCacheStore.getProductDetail(productId)?.let { snapshot ->
            val brand = brandService.findById(snapshot.brandId)
            return snapshot.toProductInfo(brand)
        }

        val product = productService.findById(productId)
        val brand = brandService.findById(product.brandId)
        return ProductInfo.of(product, brand)
            .also { productCacheStore.putProductDetail(ProductCacheSnapshot.from(product)) }
    }

    @Transactional(readOnly = true)
    fun getProductList(brandId: Long?, sortType: ProductSortType, pageable: Pageable): Page<ProductInfo> {
        productCacheStore.getProductList(brandId, sortType, pageable)?.let { snapshots ->
            return snapshots.toProductInfoPage()
        }

        val products = productService.findAll(brandId, sortType, pageable)
        val snapshots = products.map(ProductCacheSnapshot.Companion::from)
        val brandMap = loadBrandMap(snapshots.content.map { it.brandId }.distinct())
        return products.map { product ->
            val brand = brandMap[product.brandId]
                ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다: ${product.brandId}")
            ProductInfo.of(product, brand)
        }.also { productCacheStore.putProductList(brandId, sortType, pageable, snapshots) }
    }

    private fun Page<ProductCacheSnapshot>.toProductInfoPage(): Page<ProductInfo> {
        val brandMap = loadBrandMap(content.map { it.brandId }.distinct())
        return map { snapshot ->
            val brand = brandMap[snapshot.brandId]
                ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다: ${snapshot.brandId}")
            snapshot.toProductInfo(brand)
        }
    }

    private fun loadBrandMap(brandIds: List<Long>) = brandService.findAllByIds(brandIds).associateBy { it.id }
}
