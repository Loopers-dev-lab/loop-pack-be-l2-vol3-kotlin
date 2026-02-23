package com.loopers.application.catalog.product

import com.loopers.domain.catalog.CatalogCommand
import com.loopers.domain.catalog.CatalogService
import com.loopers.domain.common.Money
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class CreateProductUseCase(private val catalogService: CatalogService) {
    fun execute(brandId: Long, name: String, price: BigDecimal, stock: Int): ProductInfo {
        val product = catalogService.createProduct(
            CatalogCommand.CreateProduct(
                brandId = brandId,
                name = name,
                price = Money(price),
                stock = stock,
            ),
        )
        return ProductInfo.from(product)
    }
}
