package com.loopers.application.product

import com.loopers.domain.brand.BrandService
import com.loopers.domain.cache.CacheNames
import com.loopers.domain.cache.Cached
import com.loopers.domain.like.LikeService
import com.loopers.domain.product.CreateProductCommand
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductInfo
import com.loopers.domain.product.ProductService
import com.loopers.domain.product.ProductStatus
import com.loopers.domain.product.UpdateProductCommand
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ProductFacade(
    private val productService: ProductService,
    private val brandService: BrandService,
    private val likeService: LikeService,
) {

    fun getProduct(productId: Long): ProductResult {
        val productInfo = productService.getProductInfo(productId)
        val brandInfo = brandService.getBrandInfo(productInfo.brandId)
        return ProductResult.from(productInfo, brandInfo.name)
    }

    /**
     * 상품 목록 조회 (User) — Redis 캐시 적용.
     *
     * 목록 캐시는 명시적 evict 없이 TTL 자연 만료에 의존한다.
     * - 좋아요 1건 변경마다 brandId × sort × page 조합을 전부 evict하면 캐시가 있으나 마나
     * - 목록의 좋아요 수는 UX용 참고 지표이므로, TTL(5분) 이내 반영이면 충분
     *
     * Spring의 PageModule이 Redis ObjectMapper에 등록되어 Page<T> 직접 직렬화 가능.
     */
    @Cached(
        cacheName = CacheNames.PRODUCT_LIST,
        key = "{pageable.pageNumber}:{pageable.pageSize}:{pageable.sort}:{brandId}",
    )
    fun getProductsForUser(pageable: Pageable, brandId: Long?): Page<ProductResult> {
        val productPage = productService.findAllForUser(pageable, brandId)
        return toProductResults(productPage)
    }

    fun getProductsForAdmin(pageable: Pageable, brandId: Long?): Page<ProductResult> {
        val productPage = productService.findAllForAdmin(pageable, brandId)
        return toProductResults(productPage)
    }

    private fun toProductResults(productPage: Page<Product>): Page<ProductResult> {
        val brandIds = productPage.content.map { it.brandId }.distinct()
        val brands = brandService.findByIds(brandIds).associateBy { it.id }
        return productPage.map { product ->
            val brandName = brands[product.brandId]?.name ?: "알 수 없는 브랜드"
            ProductInfo.from(product).let { ProductResult.from(it, brandName) }
        }
    }

    fun createProduct(criteria: CreateProductCriteria): ProductResult {
        val brandInfo = brandService.getBrandInfo(criteria.brandId)
        val command = CreateProductCommand(
            brandId = criteria.brandId,
            name = criteria.name,
            description = criteria.description,
            price = criteria.price,
            stockQuantity = criteria.stockQuantity,
            displayYn = criteria.displayYn,
            imageUrl = criteria.imageUrl,
        )
        val product = productService.createProduct(command)
        return ProductInfo.from(product)
            .let { ProductResult.from(it, brandInfo.name) }
    }

    fun updateProduct(productId: Long, criteria: UpdateProductCriteria): ProductResult {
        val command = UpdateProductCommand(
            name = criteria.name,
            description = criteria.description,
            price = criteria.price,
            stockQuantity = criteria.stockQuantity,
            status = ProductStatus.valueOf(criteria.status),
            displayYn = criteria.displayYn,
            imageUrl = criteria.imageUrl,
        )
        val product = productService.updateProduct(productId, command)
        val brandInfo = brandService.getBrandInfo(product.brandId)
        return ProductInfo.from(product)
            .let { ProductResult.from(it, brandInfo.name) }
    }

    @Transactional
    fun deleteProduct(productId: Long) {
        likeService.deleteAllByProductId(productId)
        productService.resetLikeCount(productId)
        productService.deleteProduct(productId)
    }
}
