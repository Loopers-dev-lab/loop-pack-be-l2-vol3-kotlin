package com.loopers.application.admin.product

import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductService
import com.loopers.domain.product.ProductStatus
import com.loopers.domain.product.dto.ProductInfo
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional(readOnly = true)
class AdminProductFacade(
    private val productService: ProductService,
    private val brandService: BrandService,
) {

    fun getProductInfo(id: Long): ProductInfo = productService.getProductInfo(id)

    fun getProducts(brandId: Long?, pageable: Pageable): Page<ProductInfo> =
        productService.getProducts(brandId, pageable)

    @Transactional
    fun createProduct(
        brandId: Long,
        name: String,
        price: BigDecimal,
        stock: Int,
        status: ProductStatus,
    ): Long {
        val brand = brandService.getBrand(brandId)
        return productService.createProduct(
            brand = brand,
            name = name,
            price = price,
            stock = stock,
            status = status,
        )
    }

    @Transactional
    fun updateProduct(
        id: Long,
        name: String,
        price: BigDecimal,
        stock: Int,
        status: ProductStatus,
    ) {
        productService.updateProduct(
            id = id,
            name = name,
            price = price,
            stock = stock,
            status = status,
        )
    }

    @Transactional
    fun deleteProduct(id: Long) {
        productService.deleteProduct(id)
    }
}
