package com.loopers.domain.product

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
    fun findById(id: Long): ProductModel {
        return productRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다: $id")
    }

    @Transactional(readOnly = true)
    fun findAll(brandId: Long?, pageable: Pageable): Page<ProductModel> {
        return productRepository.findAllByDeletedAtIsNull(brandId, pageable)
    }

    @Transactional(readOnly = true)
    fun findAllByIds(ids: List<Long>): List<ProductModel> {
        return productRepository.findAllByIdInAndDeletedAtIsNull(ids)
    }

    @Transactional
    fun incrementLikesCount(productId: Long) {
        val product = findById(productId)
        product.incrementLikesCount()
        productRepository.save(product)
    }

    @Transactional
    fun decrementLikesCount(productId: Long) {
        val product = findById(productId)
        product.decrementLikesCount()
        productRepository.save(product)
    }
}
