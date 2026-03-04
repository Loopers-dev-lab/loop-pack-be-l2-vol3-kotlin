package com.loopers.domain.product

import com.loopers.domain.product.vo.ProductDescription
import com.loopers.domain.product.vo.ProductName
import com.loopers.domain.product.vo.ProductPrice
import com.loopers.domain.product.vo.Stock
import org.springframework.stereotype.Component

@Component
class ProductRegister(
    private val productRepository: ProductRepository,
) {

    fun register(
        brandId: Long,
        name: String,
        price: Long,
        description: String,
        stock: Int,
    ): Product {
        val product = Product(
            brandId = brandId,
            name = ProductName(name),
            price = ProductPrice(price),
            description = ProductDescription(description),
            stock = Stock(stock),
        )

        return productRepository.save(product)
    }
}
