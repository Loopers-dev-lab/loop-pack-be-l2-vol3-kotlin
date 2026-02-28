package com.loopers.domain.product

data class Stock(val quantity: Int) {

    init {
        require(quantity >= 0) {
            "재고는 0 이상이어야 합니다."
        }
    }

    fun decrease(amount: Int): Stock {
        require(amount > 0) { "차감 수량은 0보다 커야 합니다." }
        require(quantity >= amount) { "재고가 부족합니다. 현재: $quantity, 요청: $amount" }
        return Stock(quantity - amount)
    }

    fun increase(amount: Int): Stock {
        require(amount > 0) { "증가 수량은 0보다 커야 합니다." }
        return Stock(quantity + amount)
    }

    fun isEnough(amount: Int): Boolean {
        return quantity >= amount
    }
}
