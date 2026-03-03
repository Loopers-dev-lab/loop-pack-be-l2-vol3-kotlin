package com.loopers.application.catalog.product

import com.loopers.application.catalog.brand.BrandResult
import com.loopers.domain.catalog.brand.BrandService
import com.loopers.domain.catalog.product.ProductSearchCondition
import com.loopers.domain.catalog.product.ProductService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductFacade(
    private val productService: ProductService,
    private val brandService: BrandService,
) {

    @Transactional
    fun createProduct(cmd: CreateProductCommand): ProductDetailResult {
        val brand = brandService.getById(cmd.brandId) // 브랜드 존재 확인
        val product = productService.createProduct(
            brandId = cmd.brandId,
            name = cmd.name,
            description = cmd.description,
            price = cmd.price,
            stock = cmd.stock,
        )
        return ProductDetailResult(
            id = product.id,
            name = product.name,
            description = product.description,
            price = product.price,
            stock = product.stock,
            likeCount = product.likeCount,
            brand = BrandResult.from(brand),
        )
    }

    @Transactional(readOnly = true)
    fun getProductDetail(productId: Long): ProductDetailResult {
        val product = productService.getActiveById(productId)
        val brand = brandService.getById(product.brandId)
        return ProductDetailResult(
            id = product.id,
            name = product.name,
            description = product.description,
            price = product.price,
            stock = product.stock,
            likeCount = product.likeCount,
            brand = BrandResult.from(brand),
        )
    }

    @Transactional
    fun updateProduct(productId: Long, cmd: UpdateProductCommand): ProductDetailResult {
        val product = productService.update(
            id = productId,
            name = cmd.name,
            description = cmd.description,
            price = cmd.price,
            stock = cmd.stock,
        )
        val brand = brandService.getById(product.brandId)
        return ProductDetailResult(
            id = product.id,
            name = product.name,
            description = product.description,
            price = product.price,
            stock = product.stock,
            likeCount = product.likeCount,
            brand = BrandResult.from(brand),
        )
    }

    @Transactional(readOnly = true)
    fun findProducts(condition: ProductSearchCondition): List<ProductSummaryResult> =
        productService.findAll(condition).map { product ->
            val brand = brandService.getById(product.brandId)
            ProductSummaryResult.from(product, brand.name)
        }
}
