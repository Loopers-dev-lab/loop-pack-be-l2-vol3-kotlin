package com.loopers.domain.product

data class ProductName(val value: String) {

    init {
        require(value.isNotBlank()) {
            "상품명은 빈 문자열일 수 없습니다."
        }

        require(value.length <= MAX_LENGTH) {
            "상품명은 ${MAX_LENGTH}자를 초과할 수 없습니다."
        }
    }

    companion object {
        private const val MAX_LENGTH = 200
    }
}
