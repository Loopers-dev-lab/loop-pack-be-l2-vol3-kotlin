package com.loopers.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

data class Money(val amount: Long) : Comparable<Money> {

    init {
        require(amount >= 0) { "금액은 0 이상이어야 합니다." }
    }

    @JsonValue
    fun value(): Long = amount

    operator fun plus(other: Money): Money = Money(this.amount + other.amount)

    operator fun minus(other: Money): Money = Money(this.amount - other.amount)

    operator fun times(quantity: Int): Money = Money(this.amount * quantity.toLong())

    override fun compareTo(other: Money): Int = this.amount.compareTo(other.amount)

    companion object {
        val ZERO = Money(0)

        @JsonCreator
        @JvmStatic
        fun from(amount: Long): Money = Money(amount)
    }
}
