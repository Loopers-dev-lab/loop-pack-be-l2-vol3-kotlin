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
        val command = CatalogCommand.UpdateProduct(
            name = name,
            price = price,
            stock = stock,
            status = status,
        )
        val domainStatus = command.status?.let {
            Product.ProductStatus.entries.find { enum -> enum.name == it }
                ?: throw CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 상품 상태입니다: $it")
        }
        val product = productRepository.findByIdIncludeDeleted(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        product.update(command.name, command.price?.let { Money(it) }, command.stock, domainStatus)
        val saved = productRepository.save(product)
        return ProductInfo.from(saved)
    }
}
