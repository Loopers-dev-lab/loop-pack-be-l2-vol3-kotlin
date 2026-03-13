package com.loopers.application.product

import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductService
import com.loopers.support.common.PageQuery
import com.loopers.support.common.PageResult
import org.springframework.stereotype.Component

@Component
class ProductCacheManager(
    private val productLocalCacheRepository: ProductLocalCacheRepository,
    private val productCacheRepository: ProductCacheRepository,
    private val productService: ProductService,
    private val brandService: BrandService,
) {

    fun getProduct(productId: Long): ProductDetailInfo {
        return productLocalCacheRepository.getOrLoadProduct(productId) {
            loadProductDetail(productId)
        }
    }

    fun getProducts(brandId: Long?, pageQuery: PageQuery): PageResult<ProductInfo> {
        return productLocalCacheRepository.getOrLoadProducts(brandId, pageQuery) {
            loadProductList(brandId, pageQuery)
        }
    }

    fun evictProduct(productId: Long) {
        productLocalCacheRepository.evictProduct(productId)
        productCacheRepository.evictProduct(productId)
    }

    fun evictAllProducts() {
        productLocalCacheRepository.evictAllProducts()
        productCacheRepository.evictAllProducts()
    }

    fun evictAllLocalCaches() {
        productLocalCacheRepository.evictAll()
    }

    // Redis → DB 조회
    private fun loadProductDetail(productId: Long): ProductDetailInfo {
        productCacheRepository.getProduct(productId)?.let { return it }

        val product = productService.getProduct(productId)
        val brand = brandService.getBrand(product.brandId)
        val productDetailInfo = ProductDetailInfo.from(product, brand)

        productCacheRepository.setProduct(productId, productDetailInfo)
        return productDetailInfo
    }

    private fun loadProductList(brandId: Long?, pageQuery: PageQuery): PageResult<ProductInfo> {
        productCacheRepository.getProducts(brandId, pageQuery)?.let { return it }

        val pageResult = productService.getProducts(brandId, pageQuery)
            .map { ProductInfo.from(it) }

        productCacheRepository.setProducts(brandId, pageQuery, pageResult)
        return pageResult
    }
}
