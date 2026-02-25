package com.loopers.domain.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class ProductService(
    private val productRepository: ProductRepository,
) {

    fun getProduct(productId: Long): Product {
        return productRepository.findById(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
    }

    fun getAllProducts(brandId: Long?, pageable: Pageable): Page<Product> {
        return if (brandId != null) {
            productRepository.findAllByBrandId(brandId, pageable)
        } else {
            productRepository.findAll(pageable)
        }
    }

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

    fun deleteProduct(productId: Long) {
        val product = productRepository.findById(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        product.delete()
        productRepository.save(product)
    }

    fun deleteProductsByBrandId(brandId: Long) {
        val products = productRepository.findAllByBrandId(brandId)
        products.forEach {
            it.delete()
            productRepository.save(it)
        }
    }
}
