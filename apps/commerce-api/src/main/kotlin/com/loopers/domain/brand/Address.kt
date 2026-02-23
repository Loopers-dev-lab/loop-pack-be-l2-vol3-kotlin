package com.loopers.domain.brand

data class Address(
    val zipCode: String,
    val roadAddress: String,
    val detailAddress: String,
) {
    init {
        require(zipCode.matches(ZIP_CODE_PATTERN)) { "우편번호는 5자리 숫자여야 합니다." }
        require(roadAddress.isNotBlank()) { "도로명주소는 필수값 입니다." }
        require(detailAddress.isNotBlank()) { "상세주소는 필수값 입니다." }
    }

    companion object {
        val ZIP_CODE_PATTERN = """^\d{5}$""".toRegex()
    }
}
