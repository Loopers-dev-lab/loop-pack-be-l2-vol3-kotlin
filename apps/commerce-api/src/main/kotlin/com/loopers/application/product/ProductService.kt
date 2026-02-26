package com.loopers.application.product

import com.loopers.application.brand.BrandService
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
    private val brandService: BrandService,
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
    fun getProductIncludingDeleted(productId: Long): Product {
        return productRepository.findByIdIncludingDeleted(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
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
        brandService.validateBrandExists(criteria.brandId)
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
}
