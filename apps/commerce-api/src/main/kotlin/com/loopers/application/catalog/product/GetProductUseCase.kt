package com.loopers.application.catalog.product

import com.loopers.application.catalog.CatalogInfo
import com.loopers.domain.catalog.ProductDetail
import com.loopers.domain.catalog.brand.repository.BrandRepository
import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetProductUseCase(
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
) {
    @Transactional(readOnly = true)
    fun execute(productId: Long): CatalogInfo {
        val product = productRepository.findById(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        if (!product.isActive()) {
            throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        }
        val brand = brandRepository.findById(product.refBrandId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
        val detail = ProductDetail(product = product, brand = brand)
        return CatalogInfo.from(detail)
    }
}
