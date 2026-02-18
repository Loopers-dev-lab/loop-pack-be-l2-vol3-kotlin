package com.loopers.application.product

import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetProductUseCase(
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
) {
    fun getById(id: Long): ProductInfo {
        val product = productRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다: $id")
        val brand = brandRepository.findById(product.brandId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다: ${product.brandId}")
        return ProductInfo.from(product, brand)
    }

    fun getActiveById(id: Long): ProductInfo {
        val product = productRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다: $id")
        if (product.isDeleted()) {
            throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다: $id")
        }
        val brand = brandRepository.findById(product.brandId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다: ${product.brandId}")
        return ProductInfo.from(product, brand)
    }
}
