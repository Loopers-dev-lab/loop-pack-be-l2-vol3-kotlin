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

    fun getProducts(brandId: Long?, pageable: Pageable): Page<Product> {
        return productRepository.findAll(brandId, pageable)
    }

    fun getProduct(productId: Long): Product {
        return productRepository.findById(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
    }

    fun increaseLikeCount(product: Product) {
        product.increaseLikeCount()
        productRepository.save(product)
    }
}
