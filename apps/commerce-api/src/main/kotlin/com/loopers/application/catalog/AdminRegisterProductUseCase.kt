package com.loopers.application.catalog

import com.loopers.application.UseCase
import com.loopers.domain.catalog.BrandRepository
import com.loopers.domain.catalog.ProductInfo
import com.loopers.domain.catalog.ProductModel
import com.loopers.domain.catalog.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminRegisterProductUseCase(
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
) : UseCase<RegisterProductCriteria, RegisterProductResult> {

    @Transactional
    override fun execute(criteria: RegisterProductCriteria): RegisterProductResult {
        brandRepository.findById(criteria.brandId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")

        val product = ProductModel(
            brandId = criteria.brandId,
            name = criteria.name,
            quantity = criteria.quantity,
            price = criteria.price,
        )
        val saved = productRepository.save(product)
        val info = ProductInfo.from(saved)
        return RegisterProductResult.from(info)
    }
}
