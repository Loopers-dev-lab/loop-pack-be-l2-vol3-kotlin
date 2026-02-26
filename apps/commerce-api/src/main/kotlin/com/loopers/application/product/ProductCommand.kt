package com.loopers.application.product

import com.loopers.domain.product.Money
import com.loopers.domain.product.ProductSortType
import com.loopers.domain.product.Stock

class ProductCommand {

    data class Register(
        val brandId: Long,
        val name: String,
        val description: String,
        val price: Long,
        val stock: Int,
        val imageUrl: String,
    ) {
        fun toMoney(): Money = Money(price)
        fun toStock(): Stock = Stock(stock)
    }

    data class Update(
        val productId: Long,
        val name: String,
        val description: String,
        val price: Long,
        val stock: Int,
        val imageUrl: String,
    ) {
        fun toMoney(): Money = Money(price)
        fun toStock(): Stock = Stock(stock)
    }

    data class Search(
        val brandId: Long? = null,
        val sort: ProductSortType = ProductSortType.LATEST,
        val page: Int = 0,
        val size: Int = 20,
        val includeDeleted: Boolean = false,
    )
}
