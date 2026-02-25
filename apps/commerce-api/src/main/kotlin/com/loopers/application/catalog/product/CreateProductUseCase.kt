package com.loopers.application.catalog.product

import com.loopers.application.catalog.CatalogCommand
import com.loopers.domain.catalog.brand.repository.BrandRepository
import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.common.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Component
class CreateProductUseCase(
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
) {
    @Transactional
    fun execute(brandId: Long, name: String, price: BigDecimal, stock: Int): ProductInfo {
        val command = CatalogCommand.CreateProduct(
            brandId = brandId,
            name = name,
            price = price,
            stock = stock,
        )
        val brand = brandRepository.findById(command.brandId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
        val product = productRepository.save(
            Product(
                refBrandId = command.brandId,
                name = command.name,
                price = Money(command.price),
                stock = command.stock,
            ),
        )
        return ProductInfo.from(product)
    }
}
