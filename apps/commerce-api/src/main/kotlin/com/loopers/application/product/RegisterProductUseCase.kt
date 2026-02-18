package com.loopers.application.product

import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.Money
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductImage
import com.loopers.domain.product.ProductName
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.Stock
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RegisterProductUseCase(
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
) {
    @Transactional
    fun register(command: RegisterProductCommand): Long {
        val brand = brandRepository.findById(command.brandId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다: ${command.brandId}")

        brand.assertNotDeleted()

        val product = Product.create(
            brandId = command.brandId,
            name = ProductName(command.name),
            description = command.description,
            price = Money(command.price),
            stock = Stock(command.stock),
            thumbnailUrl = command.thumbnailUrl,
            images = command.images.map {
                ProductImage.create(imageUrl = it.imageUrl, displayOrder = it.displayOrder)
            },
        )

        return productRepository.save(product)
    }
}
