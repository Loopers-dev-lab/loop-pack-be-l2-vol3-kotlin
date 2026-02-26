package com.loopers.application.product

import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.CreateProductCommand
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductInfo
import com.loopers.domain.product.ProductService
import com.loopers.domain.product.ProductStatus
import com.loopers.domain.product.UpdateProductCommand
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class ProductFacade(
    private val productService: ProductService,
    private val brandService: BrandService,
) {

    fun getProduct(productId: Long): ProductResult {
        val product = productService.findById(productId)
        val brand = brandService.findById(product.brandId)
        return ProductInfo.from(product)
            .let { ProductResult.from(it, brand.name) }
    }

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
            val brand = brands[product.brandId]!!
            ProductInfo.from(product).let { ProductResult.from(it, brand.name) }
        }
    }

    fun createProduct(criteria: CreateProductCriteria): ProductResult {
        val brand = brandService.findById(criteria.brandId)
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
            .let { ProductResult.from(it, brand.name) }
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
        val brand = brandService.findById(product.brandId)
        return ProductInfo.from(product)
            .let { ProductResult.from(it, brand.name) }
    }

    fun deleteProduct(productId: Long) {
        // TODO: Step 5에서 연쇄 삭제 연결 (Like → Product)
        productService.deleteProduct(productId)
    }
}
