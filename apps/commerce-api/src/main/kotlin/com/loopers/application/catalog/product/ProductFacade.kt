package com.loopers.application.catalog.product

import com.loopers.application.catalog.brand.BrandResult
import com.loopers.domain.catalog.brand.BrandRepository
import com.loopers.domain.catalog.brand.BrandService
import com.loopers.domain.catalog.product.ProductSearchCondition
import com.loopers.domain.catalog.product.ProductService
import com.loopers.domain.catalog.product.ProductStockService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductFacade(
    private val productService: ProductService,
    private val brandService: BrandService,
    private val brandRepository: BrandRepository,
    private val productStockService: ProductStockService,
) {

    @Transactional
    fun createProduct(cmd: CreateProductCommand): ProductDetailResult {
        val brand = brandService.getById(cmd.brandId)
        val product = productService.createProduct(
            brandId = cmd.brandId,
            name = cmd.name,
            description = cmd.description,
            price = cmd.price,
        )
        val stock = productStockService.createStock(product.id, cmd.stock)
        return ProductDetailResult(
            id = product.id,
            name = product.name,
            description = product.description,
            price = product.price,
            stock = stock.quantity,
            likeCount = product.likeCount,
            brand = BrandResult.from(brand),
        )
    }

    @Transactional(readOnly = true)
    fun getProductDetail(productId: Long): ProductDetailResult {
        val product = productService.getActiveById(productId)
        val brand = brandService.getById(product.brandId)
        val stock = productStockService.getByProductId(productId)
        return ProductDetailResult(
            id = product.id,
            name = product.name,
            description = product.description,
            price = product.price,
            stock = stock.quantity,
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
        )
        val stock = productStockService.updateStock(productId, cmd.stock)
        productService.updateStockStatus(productId, stock.quantity)
        val brand = brandService.getById(product.brandId)
        return ProductDetailResult(
            id = product.id,
            name = product.name,
            description = product.description,
            price = product.price,
            stock = stock.quantity,
            likeCount = product.likeCount,
            brand = BrandResult.from(brand),
        )
    }

    @Transactional(readOnly = true)
    fun findProducts(condition: ProductSearchCondition): List<ProductSummaryResult> {
        val products = productService.findAll(condition)
        val brandIds = products.map { it.brandId }.distinct()
        val brandMap = brandRepository.findAllByIds(brandIds).associateBy { it.id }

        return products.map { product ->
            val brandName = brandMap[product.brandId]?.name ?: ""
            ProductSummaryResult.from(product, brandName)
        }
    }
}
