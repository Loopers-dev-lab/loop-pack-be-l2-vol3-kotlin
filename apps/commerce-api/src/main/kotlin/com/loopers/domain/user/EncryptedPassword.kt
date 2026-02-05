package com.loopers.domain.user

data class EncryptedPassword(val value: String) {

    init {
        require(value.isNotBlank()) {
            "EncryptedPassword는 필수값 입니다."
        }
    }
}
