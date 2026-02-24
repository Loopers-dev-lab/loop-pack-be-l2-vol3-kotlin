package com.loopers.application.catalog.product

import com.loopers.application.catalog.CatalogCommand
import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.common.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Component
class UpdateProductUseCase(private val productRepository: ProductRepository) {
    @Transactional
    fun execute(productId: Long, name: String?, price: BigDecimal?, stock: Int?, status: String?): ProductInfo {
        val domainStatus = status?.let { Product.ProductStatus.valueOf(it) }
        val command = CatalogCommand.UpdateProduct(
            name = name,
            price = price?.let { Money(it) },
            stock = stock,
            status = domainStatus,
        )
        val product = productRepository.findById(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        product.update(command.name, command.price, command.stock, command.status)
        val saved = productRepository.save(product)
        return ProductInfo.from(saved)
    }
}
