package com.loopers.application.api.product

import com.loopers.domain.product.ProductService
import com.loopers.domain.product.dto.ProductInfo
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProductFacade(
    private val productService: ProductService,
) {
    fun getProductInfo(id: Long): ProductInfo = productService.getProductInfo(id)

    fun getProducts(brandId: Long?, pageable: Pageable): Page<ProductInfo> =
        productService.getProducts(brandId, pageable)
}
