package com.loopers.application.product

import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.CreateProductCommand
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductService
import com.loopers.domain.product.UpdateProductCommand
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
    fun getProduct(productId: Long): Product {
        return productService.getProduct(productId)
    }

    @Transactional(readOnly = true)
    fun getAllProducts(brandId: Long?, pageable: Pageable): Page<Product> {
        return productService.getAllProducts(brandId, pageable)
    }

    @Transactional
    fun createProduct(command: CreateProductCommand): Product {
        brandService.getBrand(command.brandId)
        return productService.createProduct(command)
    }

    @Transactional
    fun updateProduct(productId: Long, command: UpdateProductCommand): Product {
        return productService.updateProduct(productId, command)
    }

    @Transactional
    fun deleteProduct(productId: Long) {
        productService.deleteProduct(productId)
    }
}
