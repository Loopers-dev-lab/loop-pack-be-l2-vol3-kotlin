package com.loopers.application.product

import com.loopers.domain.product.CreateProductCommand
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.UpdateProductCommand
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ProductService(
    private val productRepository: ProductRepository,
) {

    @Transactional(readOnly = true)
    fun getProduct(productId: Long): Product {
        return productRepository.findById(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
    }

    @Transactional(readOnly = true)
    fun getProductIncludingDeleted(productId: Long): Product {
        return productRepository.findByIdIncludingDeleted(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
    }

    @Transactional(readOnly = true)
    fun getAllProducts(brandId: Long?, pageable: Pageable): Page<Product> {
        return if (brandId != null) {
            productRepository.findAllByBrandId(brandId, pageable)
        } else {
            productRepository.findAll(pageable)
        }
    }

    @Transactional
    fun createProduct(command: CreateProductCommand): Product {
        return productRepository.save(
            Product(
                brandId = command.brandId,
                name = command.name,
                price = command.price,
                stock = command.stock,
                description = command.description,
                imageUrl = command.imageUrl,
            ),
        )
    }

    @Transactional
    fun updateProduct(productId: Long, command: UpdateProductCommand): Product {
        val product = productRepository.findById(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        product.update(
            name = command.name,
            price = command.price,
            stock = command.stock,
            description = command.description,
            imageUrl = command.imageUrl,
        )
        return productRepository.save(product)
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

    @Transactional
    fun deleteProductsByBrandId(brandId: Long) {
        val products = productRepository.findAllByBrandId(brandId)
        products.forEach {
            it.delete()
            productRepository.save(it)
        }
    }
}
