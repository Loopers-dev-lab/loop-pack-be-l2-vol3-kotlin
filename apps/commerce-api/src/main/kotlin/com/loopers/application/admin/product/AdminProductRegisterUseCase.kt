package com.loopers.application.admin.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStock
import com.loopers.domain.product.ProductStockRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminProductRegisterUseCase(
    private val productRepository: ProductRepository,
    private val productStockRepository: ProductStockRepository,
    private val brandRepository: BrandRepository,
) {
    @Transactional
    fun register(command: AdminProductCommand.Register): AdminProductResult.Register {
        val brand = brandRepository.findById(command.brandId)
            ?: throw CoreException(ErrorType.BRAND_NOT_FOUND)
        if (brand.status != Brand.Status.ACTIVE) {
            throw CoreException(ErrorType.BRAND_INVALID_STATUS)
        }

        val product = Product.register(
            name = command.name,
            regularPrice = Money(command.regularPrice),
            sellingPrice = Money(command.sellingPrice),
            brandId = command.brandId,
            imageUrl = command.imageUrl,
            thumbnailUrl = command.thumbnailUrl,
        )
        val savedProduct = productRepository.save(product, command.admin)

        val stock = ProductStock.create(
            productId = savedProduct.id!!,
            initialQuantity = Quantity(command.initialStock),
        )
        val savedStock = productStockRepository.save(stock, command.admin)

        return AdminProductResult.Register.from(savedProduct, savedStock)
    }
}
