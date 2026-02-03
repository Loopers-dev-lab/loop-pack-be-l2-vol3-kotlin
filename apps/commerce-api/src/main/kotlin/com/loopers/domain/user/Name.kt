package com.loopers.domain.user

data class Name (val value: String) {
    init {
        require(value.isNotBlank()) {
            "Name은 필수값 입니다."
        }
    }
}
