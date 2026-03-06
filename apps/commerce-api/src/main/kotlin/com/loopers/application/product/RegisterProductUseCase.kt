package com.loopers.application.product

import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStock
import com.loopers.domain.product.ProductStockRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ProductErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RegisterProductUseCase(
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val productStockRepository: ProductStockRepository,
) {

    @Transactional
    fun execute(command: ProductCommand.Register): ProductInfo {
        val brand = brandRepository.findActiveByIdOrNull(command.brandId)
            ?: throw CoreException(ProductErrorCode.INVALID_BRAND)

        val product = Product.create(
            brandId = brand.id,
            name = command.name,
            description = command.description,
            price = command.toMoney(),
            imageUrl = command.imageUrl,
        )
        val saved = productRepository.save(product)

        val productStock = ProductStock.create(
            productId = saved.id,
            stock = command.toStock(),
        )
        productStockRepository.save(productStock)

        return ProductInfo.from(saved, brand.name, productStock.stock.quantity)
    }
}
