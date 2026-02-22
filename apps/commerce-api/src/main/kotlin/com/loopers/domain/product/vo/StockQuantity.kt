package com.loopers.domain.product.vo

@JvmInline
value class StockQuantity(val value: Int) {
    companion object {
        fun of(value: Int): StockQuantity {
            return StockQuantity(value)
        }
    }
}
