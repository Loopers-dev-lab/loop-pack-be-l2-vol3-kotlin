package com.loopers.domain.user

data class LoginId (val value: String) {
    init {
        require(value.matches(Regex("^[a-zA-Z0-9]+\$"))) {
            "LoginId는 영문과 숫자로만 구성합니다."
        }

        require(value.isNotBlank()) {
            "LoginId는 필수 입니다."
        }
    }
}
