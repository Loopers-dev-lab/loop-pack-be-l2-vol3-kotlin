package com.loopers.application.catalog

import com.loopers.application.UseCase
import com.loopers.domain.catalog.BrandRepository
import com.loopers.domain.catalog.ProductInfo
import com.loopers.domain.catalog.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserGetProductUseCase(
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
) : UseCase<Long, UserGetProductResult> {
    @Transactional(readOnly = true)
    override fun execute(productId: Long): UserGetProductResult {
        val product = productRepository.findById(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        val brand = brandRepository.findById(product.brandId)
        val info = ProductInfo.from(product)
        return UserGetProductResult.from(info, brandName = brand?.name ?: "")
    }
}
