package com.loopers.application.api.product

import com.loopers.domain.product.ProductService
import com.loopers.domain.product.dto.ProductInfo
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProductFacade(
    private val productService: ProductService,
) {
    /**
     * 캐시된 상품 정보를 조회합니다.
     * 순수 조회만 수행하며, 조회 기록은 별도로 기록해야 합니다.
     */
    @Cacheable(value = ["product-info"], sync = true, key = "#id")
    fun getCachedProductInfo(id: Long): ProductInfo = productService.getProductInfo(id)

    /**
     * 상품 정보를 조회합니다 (캐시 적용, side-effect 없음).
     * @param id 상품 ID
     * @return 캐시된 상품 정보
     */
    fun getProductInfo(id: Long): ProductInfo = getCachedProductInfo(id)

    /**
     * 상품 정보를 조회하고 조회 기록을 남깁니다.
     * @param id 상품 ID
     * @param userId 사용자 ID (null이면 기록하지 않음)
     * @return 상품 정보
     */
    fun getProductInfo(id: Long, userId: Long?): ProductInfo {
        val productInfo = getCachedProductInfo(id)
        userId?.let { recordProductView(id, it) }
        return productInfo
    }

    /**
     * 상품 조회를 기록합니다.
     */
    fun recordProductView(id: Long, userId: Long) {
        productService.recordProductView(id, userId)
    }

    fun getActiveProducts(brandId: Long?, pageable: Pageable): Page<ProductInfo> =
        productService.getActiveProducts(brandId, pageable)
}
