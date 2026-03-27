package com.loopers.domain.payment

data class CardNo(val value: String) {
    init {
        require(FORMAT_REGEX.matches(value)) { "카드 번호 형식이 올바르지 않습니다. (예: 1234-5678-9012-3456)" }
    }

    override fun toString(): String {
        val parts = value.split("-")
        return "${parts[0]}-****-****-${parts[3]}"
    }

    companion object {
        private val FORMAT_REGEX = Regex("^\\d{4}-\\d{4}-\\d{4}-\\d{4}$")
    }
}
