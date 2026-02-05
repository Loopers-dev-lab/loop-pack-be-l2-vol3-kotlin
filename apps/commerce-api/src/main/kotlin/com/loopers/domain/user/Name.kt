package com.loopers.domain.user

data class Name(val value: String) {
    init {
        require(value.isNotBlank()) {
            "Name은 필수값 입니다."
        }
    }

    /**
     * 마지막 글자를 *로 마스킹한 이름 반환
     * 예: "김철수" → "김철*", "김" → "*"
     */
    fun masked(): String {
        return if (value.isNotEmpty()) {
            value.dropLast(1) + "*"
        } else {
            value
        }
    }
}
