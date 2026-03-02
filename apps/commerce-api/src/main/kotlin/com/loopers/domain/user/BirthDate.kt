package com.loopers.domain.user

@JvmInline
value class BirthDate(val value: String) {
    init {
        require(value.isNotBlank()) {
            "BirthDate는 필수값 입니다."
        }

        require(value.matches(BIRTH_DATE_PATTERN)) {
            "BirthDate는 YYYY-MM-DD 형식입니다."
        }
    }

    companion object {
        val BIRTH_DATE_PATTERN = """^\d{4}-\d{2}-\d{2}$""".toRegex()
    }
}
