package com.loopers.domain.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class ProductService(
    private val productRepository: ProductRepository,
) {
    @Transactional(readOnly = true)
    fun getProduct(id: Long): Product {
        return productRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw CoreException(ErrorType.NOT_FOUND)
    }

    @Transactional
    fun getProductWithLock(id: Long): Product {
        return productRepository.findByIdWithPessimisticLock(id)
            ?: throw CoreException(ErrorType.NOT_FOUND)
    }

    @Transactional(readOnly = true)
    fun getProducts(brandId: Long?, sort: ProductSort, pageable: Pageable): Page<Product> {
        return productRepository.findAllByCondition(brandId, sort, pageable)
    }

    @Transactional
    fun createProduct(brandId: Long, name: String, description: String, price: Long, stockQuantity: Int): Product {
        return productRepository.save(
            Product(
                brandId = brandId,
                name = name,
                description = description,
                price = price,
                stockQuantity = stockQuantity,
            ),
        )
    }

    @Transactional
    fun updateProduct(id: Long, name: String, description: String, price: Long, stockQuantity: Int): Product {
        val product = productRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw CoreException(ErrorType.NOT_FOUND)

        product.update(name = name, description = description, price = price, stockQuantity = stockQuantity)
        return product
    }

    @Transactional
    fun deleteProduct(id: Long) {
        val product = productRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw CoreException(ErrorType.NOT_FOUND)

        product.delete()
    }

    @Transactional
    fun increaseLikeCount(productId: Long) {
        productRepository.increaseLikeCount(productId)
    }

    @Transactional
    fun decreaseLikeCount(productId: Long) {
        productRepository.decreaseLikeCount(productId)
    }

    @Transactional
    fun softDeleteByBrandId(brandId: Long) {
        productRepository.softDeleteByBrandId(brandId)
    }
}
