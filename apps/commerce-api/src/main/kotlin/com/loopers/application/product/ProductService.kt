package com.loopers.application.product

import com.loopers.application.order.OrderItemCriteria
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ProductService(
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
) {

    @Transactional(readOnly = true)
    fun getProduct(productId: Long): Product {
        return productRepository.findById(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
    }

    @Transactional(readOnly = true)
    fun getProductInfo(productId: Long): ProductInfo {
        return ProductInfo.from(getProduct(productId))
    }

    @Transactional(readOnly = true)
    fun validateProductExistsIncludingDeleted(productId: Long) {
        if (!productRepository.existsByIdIncludingDeleted(productId)) {
            throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        }
    }

    @Transactional(readOnly = true)
    fun getAllProducts(brandId: Long?, pageable: Pageable): Page<ProductInfo> {
        val products = if (brandId != null) {
            productRepository.findAllByBrandId(brandId, pageable)
        } else {
            productRepository.findAll(pageable)
        }
        return products.map { ProductInfo.from(it) }
    }

    @Transactional
    fun createProduct(criteria: CreateProductCriteria): ProductInfo {
        if (!brandRepository.existsById(criteria.brandId)) {
            throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
        }
        val product = productRepository.save(
            Product(
                brandId = criteria.brandId,
                name = criteria.name,
                price = criteria.price,
                stock = criteria.stock,
                description = criteria.description,
                imageUrl = criteria.imageUrl,
            ),
        )
        return ProductInfo.from(product)
    }

    @Transactional
    fun updateProduct(productId: Long, criteria: UpdateProductCriteria): ProductInfo {
        val product = productRepository.findById(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        product.update(
            name = criteria.name,
            price = criteria.price,
            stock = criteria.stock,
            description = criteria.description,
            imageUrl = criteria.imageUrl,
        )
        val savedProduct = productRepository.save(product)
        return ProductInfo.from(savedProduct)
    }

    @Transactional
    fun deleteProduct(productId: Long) {
        val product = productRepository.findById(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        product.delete()
        productRepository.save(product)
    }

    @Transactional
    fun getProductsWithLock(productIds: List<Long>): List<Product> {
        return productRepository.findAllByIdWithLock(productIds)
    }

    fun reserveStock(
        products: List<Product>,
        criteria: List<OrderItemCriteria>,
    ): List<ReservedProduct> {
        val productMap = products.associateBy { it.id }
        val failedReasons = mutableListOf<String>()

        for (item in criteria) {
            val product = productMap[item.productId]
            if (product == null) {
                failedReasons.add("상품 ID ${item.productId}: 존재하지 않는 상품입니다.")
            } else if (!product.hasEnoughStock(item.quantity)) {
                failedReasons.add("상품 ID ${item.productId}: 재고가 부족합니다. 현재 재고: ${product.stock}")
            }
        }

        if (failedReasons.isNotEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "재고가 부족한 상품이 있습니다. ${failedReasons.joinToString(", ")}")
        }

        return criteria.map { item ->
            val product = productMap[item.productId]!!
            product.reserve(item.quantity)
            ReservedProduct(product.id, product.name, product.brandId, item.quantity, product.price)
        }
    }
}
