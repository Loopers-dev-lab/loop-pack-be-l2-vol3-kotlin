package com.loopers.domain.user

data class BirthDate (val value: String) {
    init {
        require(value.matches(Regex("""^\d{4}-\d{2}-\d{2}$"""))) {
            "BirthDate는 0000-00-00 형식입니다."
        }

        require(value.isNotBlank()) {
            "BirthDate는 필수값 입니다."
        }
    }
}
