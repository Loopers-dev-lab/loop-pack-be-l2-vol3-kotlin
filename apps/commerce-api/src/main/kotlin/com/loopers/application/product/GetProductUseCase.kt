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
    private val productCachePort: ProductCachePort,
) {
    fun getById(id: Long): ProductInfo {
        val product = productRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다: $id")
        val brand = brandRepository.findById(product.refBrandId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다: ${product.refBrandId}")
        return ProductInfo.from(product, brand)
    }

    fun getActiveById(id: Long): ProductInfo {
        productCachePort.getProductDetail(id)?.let { return it }

        val product = productRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다: $id")
        if (product.isDeleted()) {
            throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다: $id")
        }
        val brand = brandRepository.findById(product.refBrandId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다: ${product.refBrandId}")
        val productInfo = ProductInfo.from(product, brand)
        productCachePort.setProductDetail(id, productInfo)
        return productInfo
    }
}
