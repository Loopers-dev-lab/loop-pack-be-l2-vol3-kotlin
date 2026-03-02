package com.loopers.domain.user

@JvmInline
value class Name(val value: String) {
    init {
        require(value.isNotBlank()) {
            "Name은 필수값 입니다."
        }

        require(value.matches(NAME_PATTERN)) {
            "Name은 한글과 영문 그리고 영문 사이 당 공백 하나로 이루어져야 합니다."
        }
    }

    companion object {
        val NAME_PATTERN = """^[가-힣a-zA-Z]+( [가-힣a-zA-Z]+)*$""".toRegex()
    }

    /**
     * 마지막 글자를 *로 마스킹한 이름 반환
     * 예: "홍길동" → "홍길*", "김" → "*"
     */
    fun masked(): String {
        return if (value.isNotEmpty()) {
            value.dropLast(1) + "*"
        } else {
            value
        }
    }
}
