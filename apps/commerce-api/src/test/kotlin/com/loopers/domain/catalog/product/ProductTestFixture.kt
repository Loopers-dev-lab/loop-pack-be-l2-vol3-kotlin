package com.loopers.domain.catalog.product

import com.loopers.domain.catalog.product.entity.Product
import com.loopers.domain.common.Money
import java.math.BigDecimal

object ProductTestFixture {

    const val DEFAULT_BRAND_ID = 1L
    const val DEFAULT_NAME = "에어맥스 90"
    val DEFAULT_PRICE: BigDecimal = BigDecimal("129000")
    const val DEFAULT_STOCK = 100

    fun createProduct(
        refBrandId: Long = DEFAULT_BRAND_ID,
        name: String = DEFAULT_NAME,
        price: BigDecimal = DEFAULT_PRICE,
        stock: Int = DEFAULT_STOCK,
    ): Product = Product(refBrandId = refBrandId, name = name, price = Money(price), stock = stock)
}
