package com.loopers.application.product

import com.loopers.domain.brand.BrandService
import com.loopers.domain.common.Money
import com.loopers.support.common.PageQuery
import com.loopers.support.common.PageResult
import com.loopers.domain.common.StockQuantity
import com.loopers.domain.product.ProductService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminProductFacade(
    private val productService: ProductService,
    private val brandService: BrandService,
) {
    @Transactional(readOnly = true)
    fun getProducts(brandId: Long?, pageQuery: PageQuery): PageResult<AdminProductInfo> {
        val productPage = productService.getProducts(brandId, pageQuery)
        val brandIds = productPage.content.map { it.brandId }.distinct()
        val brandMap = if (brandIds.isNotEmpty()) {
            brandService.getBrandsByIds(brandIds).associateBy { it.id }
        } else {
            emptyMap()
        }
        return productPage.map { product ->
            AdminProductInfo.from(product, brandMap.getValue(product.brandId))
        }
    }

    @Transactional(readOnly = true)
    fun getProductDetail(productId: Long): AdminProductInfo {
        val product = productService.getProduct(productId)
        val brand = brandService.getBrand(product.brandId)
        return AdminProductInfo.from(product, brand)
    }

    @Transactional
    fun updateProduct(
        productId: Long,
        name: String,
        description: String?,
        price: Long,
        stockQuantity: Int,
        brandId: Long,
    ): AdminProductInfo {
        val product = productService.getProduct(productId)
        productService.updateProduct(product, name, description, Money.of(price), StockQuantity.of(stockQuantity), brandId)
        val brand = brandService.getBrand(product.brandId)
        return AdminProductInfo.from(product, brand)
    }

    @Transactional
    fun deleteProduct(productId: Long) {
        val product = productService.getProduct(productId)
        productService.delete(product)
    }

    @Transactional
    fun createProduct(
        name: String,
        description: String?,
        price: Long,
        stockQuantity: Int,
        brandId: Long,
    ): AdminProductInfo {
        val brand = brandService.getBrand(brandId)
        val product = productService.createProduct(
            name = name,
            description = description,
            price = Money.of(price),
            stockQuantity = StockQuantity.of(stockQuantity),
            brandId = brandId,
        )
        return AdminProductInfo.from(product, brand)
    }
}
