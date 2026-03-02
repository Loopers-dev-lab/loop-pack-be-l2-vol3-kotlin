package com.loopers.domain.user

@JvmInline
value class LoginId(val value: String) {
    init {
        require(value.isNotBlank()) {
            "LoginId는 필수 입니다."
        }

        require(value.matches(LOGIN_ID_PATTERN)) {
            "LoginId는 영문과 숫자로만 구성합니다."
        }
    }

    companion object {
        val LOGIN_ID_PATTERN = """^[a-zA-Z0-9]+$""".toRegex()
    }
}
