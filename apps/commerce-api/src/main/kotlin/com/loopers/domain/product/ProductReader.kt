package com.loopers.domain.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class ProductReader(
    private val productRepository: ProductRepository,
) {

    fun getById(id: Long): Product {
        return productRepository.findById(id)
            ?: throw CoreException(ErrorType.PRODUCT_NOT_FOUND)
    }

    fun getSellingById(id: Long): Product {
        val product = getById(id)
        if (product.status != ProductStatus.SELLING) {
            throw CoreException(ErrorType.PRODUCT_ALREADY_STOP_SELLING)
        }
        return product
    }

    fun getAll(): List<Product> {
        return productRepository.findAll()
    }

    fun getAllByBrandId(brandId: Long): List<Product> {
        return productRepository.findAllByBrandId(brandId)
    }

    fun getAllByIds(ids: List<Long>): List<Product> {
        return productRepository.findAllByIds(ids)
    }

    fun existsSellingByBrandId(brandId: Long): Boolean {
        return productRepository.existsByBrandIdAndStatus(brandId, ProductStatus.SELLING)
    }
}
