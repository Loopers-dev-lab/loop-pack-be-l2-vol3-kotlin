package com.loopers.application.product

import com.loopers.application.brand.BrandService
import com.loopers.domain.common.PageResult
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminProductFacade(
    private val productService: ProductService,
    private val brandService: BrandService,
) {
    @Transactional
    fun createProduct(command: ProductCommand.Create): ProductInfo {
        val brand = brandService.getBrandForAdmin(command.brandId)
        val product = productService.createProduct(command)
        return ProductInfo.from(product, brand.name)
    }

    @Transactional(readOnly = true)
    fun getProduct(id: Long): ProductInfo {
        val product = productService.getProductForAdmin(id)
        val brand = brandService.getBrandForAdmin(product.brandId)
        return ProductInfo.from(product, brand.name)
    }

    @Transactional(readOnly = true)
    fun getProducts(page: Int, size: Int, brandId: Long?): PageResult<ProductInfo> {
        val result = productService.getProductsForAdmin(page, size, brandId)
        val brandIds = result.content.map { it.brandId }.distinct()
        val brandMap = brandIds.associateWith { id ->
            runCatching { brandService.getBrandForAdmin(id) }.getOrNull()?.name
        }
        return PageResult(
            content = result.content.map { ProductInfo.from(it, brandMap[it.brandId]) },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    @Transactional
    fun updateProduct(id: Long, command: ProductCommand.Update): ProductInfo {
        val product = productService.updateProduct(id, command)
        val brand = brandService.getBrandForAdmin(product.brandId)
        return ProductInfo.from(product, brand.name)
    }

    @Transactional
    fun deleteProduct(id: Long) {
        productService.deleteProduct(id)
    }
}
