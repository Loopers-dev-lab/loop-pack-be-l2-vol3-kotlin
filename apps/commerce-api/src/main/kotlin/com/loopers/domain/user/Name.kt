package com.loopers.domain.user

data class Name(val value: String) {
    init {
        require(value.isNotBlank()) { "이름은 빈 문자열일 수 없습니다." }
    }

    fun masked(): String {
        return if (value.length == 1) {
            "*"
        } else {
            value.dropLast(1) + "*"
        }
    }
}
