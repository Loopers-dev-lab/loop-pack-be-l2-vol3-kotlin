package com.loopers.domain.brand

@JvmInline
value class Email(val value: String) {
    init {
        require(value.isNotBlank()) {
            "Email은 필수값 입니다."
        }

        require(value.matches(EMAIL_PATTERN)) {
            "Email 형식(****@**.**)을 만족해야합니다."
        }
    }

    companion object {
        val EMAIL_PATTERN = """^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$""".toRegex()
    }
}
