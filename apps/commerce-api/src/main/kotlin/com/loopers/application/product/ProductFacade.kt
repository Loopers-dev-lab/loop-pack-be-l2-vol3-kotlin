package com.loopers.application.product

import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class ProductFacade(
    private val productService: ProductService,
    private val brandService: BrandService,
) {

    fun getProducts(brandId: Long?, pageable: Pageable): Page<ProductInfo> {
        return productService.getProducts(brandId, pageable)
            .map { ProductInfo.from(it) }
    }

    fun getProduct(productId: Long): ProductDetailInfo {
        val product = productService.getProduct(productId)
        val brand = brandService.getBrand(product.brandId)
        return ProductDetailInfo.from(product, brand)
    }
}
