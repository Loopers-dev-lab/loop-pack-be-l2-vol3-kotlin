package com.loopers.application.product

import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductCommand
import com.loopers.domain.product.ProductService
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

@Component
class AdminProductFacade(
    private val productService: ProductService,
    private val brandService: BrandService,
) {
    fun createProduct(command: ProductCommand.Create): ProductInfo {
        val brand = brandService.getBrandForAdmin(command.brandId)
        val product = productService.createProduct(command)
        return ProductInfo.from(product, brand.name)
    }

    fun getProduct(id: Long): ProductInfo {
        val product = productService.getProductForAdmin(id)
        val brand = brandService.getBrandForAdmin(product.brandId)
        return ProductInfo.from(product, brand.name)
    }

    fun getProducts(page: Int, size: Int, brandId: Long?): Page<ProductInfo> {
        val products = productService.getProductsForAdmin(page, size, brandId)
        val brandIds = products.content.map { it.brandId }.distinct()
        val brandMap = brandIds.associateWith { id ->
            runCatching { brandService.getBrandForAdmin(id) }.getOrNull()?.name
        }
        return products.map { ProductInfo.from(it, brandMap[it.brandId]) }
    }

    fun updateProduct(id: Long, command: ProductCommand.Update): ProductInfo {
        val product = productService.updateProduct(id, command)
        val brand = brandService.getBrandForAdmin(product.brandId)
        return ProductInfo.from(product, brand.name)
    }

    fun deleteProduct(id: Long) {
        productService.deleteProduct(id)
    }
}
