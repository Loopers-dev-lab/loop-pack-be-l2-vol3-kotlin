package com.loopers.domain.user

data class Password (val value: String) {

    init {
        require(value.matches(Regex("^[a-zA-Z0-9!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]{8,16}\$"))) {
            "Password는 8~16자의 영문 대소문자, 숫자, 특수문자로만 구성합니다."
        }

        require(value.isNotBlank()) {
            "Password는 필수 입니다."
        }
    }
}
