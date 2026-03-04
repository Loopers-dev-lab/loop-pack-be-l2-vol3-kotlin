package com.loopers.domain.product

import com.loopers.domain.product.vo.ProductDescription
import com.loopers.domain.product.vo.ProductName
import com.loopers.domain.product.vo.ProductPrice
import com.loopers.domain.product.vo.Stock
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class Product(
    val id: Long? = null,
    val brandId: Long,
    name: ProductName,
    price: ProductPrice,
    description: ProductDescription,
    stock: Stock,
    status: ProductStatus = ProductStatus.SELLING,
) {
    var name: ProductName = name
        private set

    var price: ProductPrice = price
        private set

    var description: ProductDescription = description
        private set

    var stock: Stock = stock
        private set

    var status: ProductStatus = status
        private set

    fun changeInfo(name: ProductName, price: ProductPrice, description: ProductDescription) {
        this.name = name
        this.price = price
        this.description = description
    }

    fun stopSelling() {
        if (status == ProductStatus.STOP_SELLING) {
            throw CoreException(ErrorType.PRODUCT_ALREADY_STOP_SELLING)
        }
        this.status = ProductStatus.STOP_SELLING
    }

    fun deductStock(quantity: Int) {
        this.stock = stock.deduct(quantity)
    }

    fun restoreStock(quantity: Int) {
        this.stock = stock.restore(quantity)
    }
}
