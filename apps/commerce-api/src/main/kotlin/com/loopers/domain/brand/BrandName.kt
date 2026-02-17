package com.loopers.domain.brand

data class BrandName(val value: String) {

    init {
        require(value.isNotBlank()) {
            "브랜드 이름은 빈 문자열일 수 없습니다."
        }

        require(value.length <= MAX_LENGTH) {
            "브랜드 이름은 ${MAX_LENGTH}자를 초과할 수 없습니다."
        }
    }

    companion object {
        private const val MAX_LENGTH = 100
    }
}
