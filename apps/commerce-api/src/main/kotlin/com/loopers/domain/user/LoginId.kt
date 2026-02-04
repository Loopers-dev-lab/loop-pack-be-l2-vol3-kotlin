package com.loopers.domain.user

data class LoginId(val value: String) {
    init {
        require(value.isNotBlank()) { "로그인 ID는 빈 문자열일 수 없습니다." }
        require(value.length <= MAX_LENGTH) { "로그인 ID는 ${MAX_LENGTH}자 이내여야 합니다." }
        require(FORMAT_REGEX.matches(value)) { "로그인 ID는 영문과 숫자만 허용됩니다." }
    }

    companion object {
        private const val MAX_LENGTH = 10
        private val FORMAT_REGEX = Regex("^[a-zA-Z0-9]+$")
    }
}
