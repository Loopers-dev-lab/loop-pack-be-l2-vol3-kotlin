package com.loopers.domain.user

data class Email(val value: String) {
    init {
        require(value.isNotBlank()) { "이메일은 빈 문자열일 수 없습니다." }

        require(FORMAT_REGEX.matches(value)) { "올바른 이메일 형식이 아닙니다." }
    }

    companion object {
        private val FORMAT_REGEX = Regex("^[^@]+@[^@]+\\.[^@]+$")
    }
}
