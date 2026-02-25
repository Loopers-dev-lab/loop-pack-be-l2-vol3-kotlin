package com.loopers.domain.product

import com.loopers.domain.product.vo.ProductDescription
import com.loopers.domain.product.vo.ProductName
import com.loopers.domain.product.vo.ProductPrice
import org.springframework.stereotype.Component

@Component
class ProductChanger(
    private val productReader: ProductReader,
    private val productRepository: ProductRepository,
) {

    fun changeInfo(id: Long, name: String, price: Long, description: String): Product {
        val product = productReader.getById(id)

        product.changeInfo(
            name = ProductName(name),
            price = ProductPrice(price),
            description = ProductDescription(description),
        )

        return productRepository.save(product)
    }
}
