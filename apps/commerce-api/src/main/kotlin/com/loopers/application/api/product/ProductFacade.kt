package com.loopers.application.api.product

import com.loopers.domain.product.ProductService
import com.loopers.domain.product.dto.ProductInfo
import com.loopers.domain.product.event.ProductViewedEvent
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProductFacade(
    private val productService: ProductService,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    @Cacheable(value = ["product-info"], sync = true, key = "#id")
    fun getProductInfo(id: Long, userId: Long? = null): ProductInfo {
        val productInfo = productService.getProductInfo(id)

        userId?.let {
            applicationEventPublisher.publishEvent(
                ProductViewedEvent(
                    productId = id,
                    userId = it,
                ),
            )
        }

        return productInfo
    }

    fun getActiveProducts(brandId: Long?, pageable: Pageable): Page<ProductInfo> =
        productService.getActiveProducts(brandId, pageable)
}
