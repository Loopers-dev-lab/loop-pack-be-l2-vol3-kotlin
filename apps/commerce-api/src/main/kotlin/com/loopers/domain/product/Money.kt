package com.loopers.domain.product

data class Money(val amount: Long) {

    init {
        require(amount >= 0) {
            "금액은 0 이상이어야 합니다."
        }
    }

    fun multiply(quantity: Int): Money {
        return Money(amount * quantity)
    }

    fun add(other: Money): Money {
        return Money(amount + other.amount)
    }
}
