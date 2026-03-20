package com.loopers.domain.payment.vo

@JvmInline
value class CardNo(val value: String) {
    companion object {
        private val REGEX = Regex("^\\d{4}-\\d{4}-\\d{4}-\\d{4}$")

        fun of(value: String): CardNo {
            require(REGEX.matches(value)) { "카드 번호는 xxxx-xxxx-xxxx-xxxx 형식이어야 합니다." }
            return CardNo(value)
        }
    }
}
