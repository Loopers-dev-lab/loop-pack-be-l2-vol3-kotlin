package com.loopers.application.catalog.product

import com.loopers.domain.catalog.CatalogCommand
import com.loopers.domain.catalog.CatalogService
import com.loopers.domain.catalog.product.entity.Product
import com.loopers.domain.common.Money
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class UpdateProductUseCase(private val catalogService: CatalogService) {
    fun execute(productId: Long, name: String?, price: BigDecimal?, stock: Int?, status: String?): ProductInfo {
        val domainStatus = status?.let { Product.ProductStatus.valueOf(it) }
        val product = catalogService.updateProduct(
            productId,
            CatalogCommand.UpdateProduct(
                name = name,
                price = price?.let { Money(it) },
                stock = stock,
                status = domainStatus,
            ),
        )
        return ProductInfo.from(product)
    }
}
