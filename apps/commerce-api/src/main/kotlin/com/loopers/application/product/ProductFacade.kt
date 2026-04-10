package com.loopers.application.product

import com.loopers.application.brand.BrandInfo
import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductService
import com.loopers.domain.product.ProductSort
import com.loopers.domain.ranking.RankingService
import com.loopers.infrastructure.cache.ProductCacheDto
import com.loopers.infrastructure.cache.ProductCacheRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class ProductFacade(
    private val productService: ProductService,
    private val brandService: BrandService,
    private val productCacheRepository: ProductCacheRepository,
    private val rankingService: RankingService,
) {
    fun getProduct(id: Long): ProductInfo {
        val cached = productCacheRepository.get(id)
        val productInfo = if (cached != null) {
            cached.toProductInfo()
        } else {
            val product = productService.getProduct(id)
            val brandInfo = BrandInfo.from(brandService.getBrand(product.brandId))
            val info = ProductInfo.from(product, brandInfo)
            productCacheRepository.put(id, ProductCacheDto.from(info))
            info
        }

        val rank = rankingService.getProductRank(id)
        return productInfo.copy(rank = rank)
    }

    fun getProducts(brandId: Long?, sort: ProductSort, pageable: Pageable): Page<ProductInfo> {
        val products = productService.getProducts(brandId, sort, pageable)
        val brandIds = products.content.map { it.brandId }.distinct()
        val brandMap = brandService.getBrandsByIds(brandIds).associate { it.id to BrandInfo.from(it) }

        return products.map { product ->
            ProductInfo.from(product, brandMap[product.brandId]!!)
        }
    }

    fun createProduct(brandId: Long, name: String, description: String, price: Long, stockQuantity: Int): ProductInfo {
        val brand = brandService.getBrand(brandId)
        val product = productService.createProduct(brandId, name, description, price, stockQuantity)
        return ProductInfo.from(product, BrandInfo.from(brand))
    }

    fun updateProduct(id: Long, name: String, description: String, price: Long, stockQuantity: Int): ProductInfo {
        val product = productService.updateProduct(id, name, description, price, stockQuantity)
        val brandInfo = BrandInfo.from(brandService.getBrand(product.brandId))
        val productInfo = ProductInfo.from(product, brandInfo)

        productCacheRepository.evict(id)
        return productInfo
    }

    fun deleteProduct(id: Long) {
        productService.deleteProduct(id)
        productCacheRepository.evict(id)
    }
}
