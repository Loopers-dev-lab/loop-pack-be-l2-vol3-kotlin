package com.loopers.domain.user

data class Email (val value: String) {
    init {
        require(value.matches(Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$"))) {
            "Email 형식(****@**.**)을 만족해야합니다."
        }

        require(value.isNotBlank()) {
            "Email은 필수값 입니다."
        }
    }
}
