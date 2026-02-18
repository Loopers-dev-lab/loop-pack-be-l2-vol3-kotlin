package com.loopers.application.product

import com.loopers.domain.product.Money
import com.loopers.domain.product.ProductImage
import com.loopers.domain.product.ProductName
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStatus
import com.loopers.domain.product.Stock
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UpdateProductUseCase(
    private val productRepository: ProductRepository,
) {
    @Transactional
    fun update(id: Long, command: UpdateProductCommand): ProductInfo {
        val product = productRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다: $id")

        val updatedProduct = product.update(
            name = ProductName(command.name),
            description = command.description,
            price = Money(command.price),
            stock = Stock(command.stock),
            thumbnailUrl = command.thumbnailUrl,
            status = ProductStatus.valueOf(command.status),
            images = command.images.map {
                ProductImage.create(imageUrl = it.imageUrl, displayOrder = it.displayOrder)
            },
        )

        productRepository.save(updatedProduct)
        return ProductInfo.from(updatedProduct)
    }
}
