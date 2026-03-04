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

    fun subtract(other: Money): Money {
        return Money(maxOf(amount - other.amount, 0))
    }

    fun percentage(rate: Long): Money {
        return Money(amount * rate / 100)
    }

    fun min(other: Money): Money {
        return if (amount <= other.amount) this else other
    }
}
