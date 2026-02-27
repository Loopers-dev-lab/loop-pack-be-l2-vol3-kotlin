package com.loopers.application.product

import com.loopers.domain.brand.BrandService
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
) {
    @Transactional(readOnly = true)
    fun getProductDetail(productId: Long): ProductInfo {
        val product = productService.findById(productId)
        val brand = brandService.findById(product.brandId)
        return ProductInfo.of(product, brand)
    }

    @Transactional(readOnly = true)
    fun getProductList(brandId: Long?, pageable: Pageable): Page<ProductInfo> {
        val products = productService.findAll(brandId, pageable)
        val brandIds = products.content.map { it.brandId }.distinct()
        val brandMap = brandService.findAllByIds(brandIds).associateBy { it.id }
        return products.map { product ->
            val brand = brandMap[product.brandId]
                ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다: ${product.brandId}")
            ProductInfo.of(product, brand)
        }
    }
}
