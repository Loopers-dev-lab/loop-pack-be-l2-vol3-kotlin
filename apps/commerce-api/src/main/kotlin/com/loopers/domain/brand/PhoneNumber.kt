package com.loopers.domain.brand

@JvmInline
value class PhoneNumber(val value: String) {
    init {
        require(value.matches(PHONE_NUMBER_PATTERN)) { "전화번호는 XXX-XXXX-XXXX 또는 XX-XXX-XXXX 형식이어야 합니다." }
    }

    companion object {
        val PHONE_NUMBER_PATTERN = """^\d{2,3}-\d{3,4}-\d{4}$""".toRegex()
    }
}
