package com.loopers.application.product

import com.loopers.domain.product.vo.ProductDescription
import com.loopers.domain.product.vo.ProductName
import com.loopers.domain.product.vo.StockQuantity

class ProductCommand {
    data class Create(
        val brandId: Long,
        val name: String,
        val description: String,
        val price: Long,
        val stockQuantity: Int,
        val imageUrl: String,
    ) {
        init {
            ProductName.of(name)
            ProductDescription.of(description)
            StockQuantity.of(stockQuantity)
        }
    }

    data class Update(
        val name: String,
        val description: String,
        val price: Long,
        val stockQuantity: Int,
        val imageUrl: String,
    ) {
        init {
            ProductName.of(name)
            ProductDescription.of(description)
            StockQuantity.of(stockQuantity)
        }
    }
}
