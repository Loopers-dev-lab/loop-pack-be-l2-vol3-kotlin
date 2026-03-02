package com.loopers.domain.catalog.product

import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.catalog.product.vo.Stock
import com.loopers.domain.common.vo.BrandId
import com.loopers.domain.common.vo.Money
import java.math.BigDecimal
import java.time.ZonedDateTime

object ProductTestFixture {

    val DEFAULT_BRAND_ID: BrandId = BrandId(1)
    const val DEFAULT_NAME = "에어맥스 90"
    val DEFAULT_PRICE: BigDecimal = BigDecimal("129000")
    const val DEFAULT_STOCK = 100

    fun createProduct(
        refBrandId: BrandId = DEFAULT_BRAND_ID,
        name: String = DEFAULT_NAME,
        price: BigDecimal = DEFAULT_PRICE,
        stock: Int = DEFAULT_STOCK,
        deletedAt: ZonedDateTime? = null,
    ): Product = Product(refBrandId = refBrandId, name = name, price = Money(price), stock = Stock(stock), deletedAt = deletedAt)
}
