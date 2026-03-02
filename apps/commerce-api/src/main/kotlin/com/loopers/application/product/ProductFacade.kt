package com.loopers.application.product

import com.loopers.application.brand.BrandInfo
import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductInventoryService
import com.loopers.domain.product.ProductService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ProductFacade(
    private val productService: ProductService,
    private val productInventoryService: ProductInventoryService,
    private val brandService: BrandService,
) {
    @Transactional
    fun createProduct(
        brandId: Long,
        name: String,
        imageUrl: String,
        description: String,
        price: Long,
        quantity: Long,
    ): ProductInfo {
        val product = productService.createProduct(brandId, name, imageUrl, description, price)
        productInventoryService.createInventory(product.id, quantity)
        val brand = BrandInfo.from(brandService.getBrandById(product.brandId))
        return ProductInfo.from(product, brand)
    }

    fun getProducts(brandId: Long?, pageable: Pageable): Page<ProductInfo> {
        val products = productService.getProducts(brandId, pageable)
        return products.map { product ->
            val brand = BrandInfo.from(brandService.getBrandById(product.brandId))
            ProductInfo.from(product, brand)
        }
    }

    fun getProductById(id: Long): ProductInfo {
        val product = productService.getProductById(id)
        val brand = BrandInfo.from(brandService.getBrandById(product.brandId))
        return ProductInfo.from(product, brand)
    }

    @Transactional
    fun updateProduct(
        id: Long,
        name: String,
        imageUrl: String,
        description: String,
        price: Long,
        quantity: Long,
    ): ProductInfo {
        val product = productService.updateProduct(id, name, imageUrl, description, price)
        productInventoryService.updateStock(id, quantity)
        val brand = BrandInfo.from(brandService.getBrandById(product.brandId))
        return ProductInfo.from(product, brand)
    }

    @Transactional
    fun deleteProduct(id: Long) {
        productService.deleteProduct(id)
        productInventoryService.deleteInventory(id)
    }
}
