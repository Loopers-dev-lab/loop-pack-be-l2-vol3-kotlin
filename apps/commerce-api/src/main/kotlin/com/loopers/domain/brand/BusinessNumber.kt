package com.loopers.domain.brand

@JvmInline
value class BusinessNumber(val value: String) {
    init {
        require(value.matches(BUSINESS_NUMBER_PATTERN)) { "사업자번호는 XXX-XX-XXXXX 형식이어야 합니다." }
    }

    companion object {
        // XXX  - XX - XXXXX
        // 세무서 - 구분 - 일련번호
        val BUSINESS_NUMBER_PATTERN = """^\d{3}-\d{2}-\d{5}$""".toRegex()
    }
}
