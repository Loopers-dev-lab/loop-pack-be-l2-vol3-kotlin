package com.loopers.application.product

import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ProductErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UpdateProductUseCase(
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
) {

    @Transactional
    fun execute(command: ProductCommand.Update): ProductInfo {
        val product = productRepository.findActiveByIdOrNull(command.productId)
            ?: throw CoreException(ProductErrorCode.PRODUCT_NOT_FOUND)

        product.update(
            name = command.name,
            description = command.description,
            price = command.toMoney(),
            stock = command.toStock(),
            imageUrl = command.imageUrl,
        )
        val saved = productRepository.save(product)

        val brand = brandRepository.findByIdOrNull(saved.brandId)
        val brandName = brand?.name ?: ""

        return ProductInfo.from(saved, brandName)
    }
}
