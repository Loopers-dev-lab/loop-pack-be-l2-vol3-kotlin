package com.loopers.application.product

import com.loopers.application.brand.BrandService
import com.loopers.domain.product.CreateProductCommand
import com.loopers.domain.product.Product
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ProductFacade(
    private val productService: ProductService,
    private val brandService: BrandService,
) {

    @Transactional
    fun createProduct(command: CreateProductCommand): Product {
        brandService.getBrand(command.brandId)
        return productService.createProduct(command)
    }
}
