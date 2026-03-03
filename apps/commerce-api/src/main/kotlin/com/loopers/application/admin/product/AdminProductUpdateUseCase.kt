package com.loopers.application.admin.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminProductUpdateUseCase(
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
) {
    @Transactional
    fun update(command: AdminProductCommand.Update): AdminProductResult.Update {
        val product = productRepository.findById(command.productId)
            ?: throw CoreException(ErrorType.PRODUCT_NOT_FOUND)

        val updated = product.changeInfo(
            name = command.name,
            regularPrice = Money(command.regularPrice),
            sellingPrice = Money(command.sellingPrice),
            imageUrl = command.imageUrl,
            thumbnailUrl = command.thumbnailUrl,
        )

        val targetStatus = Product.Status.valueOf(command.status)
        val statusChanged = applyStatusChange(updated, targetStatus)

        val saved = productRepository.save(statusChanged, command.admin)
        return AdminProductResult.Update.from(saved)
    }

    private fun applyStatusChange(product: Product, targetStatus: Product.Status): Product {
        if (product.status == targetStatus) return product

        return when (targetStatus) {
            Product.Status.ACTIVE -> {
                val brand = brandRepository.findById(product.brandId)
                    ?: throw CoreException(ErrorType.BRAND_NOT_FOUND)
                if (brand.status != Brand.Status.ACTIVE) {
                    throw CoreException(ErrorType.PRODUCT_INVALID_STATUS)
                }
                product.activate()
            }
            Product.Status.INACTIVE -> product.deactivate()
        }
    }
}
