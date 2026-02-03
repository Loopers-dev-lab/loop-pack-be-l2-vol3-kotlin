package com.loopers.domain.user

data class User (
    val loginId: LoginId,
    val password: Password,
    val name: Name,
    val birthDate: BirthDate,
    val email: Email,
) {
    init {
        // 비밀번호 내에 생년월일 포함 X 검증
        validate(password, birthDate)
    }

    private fun validate(password: Password, birthDate: BirthDate) {
        val fullBirthDate = birthDate.value.replace("-", "") // "20020101"
        val shortBirthDate = fullBirthDate.substring(2) // "020101"

        if (password.value.contains(fullBirthDate) || password.value.contains(shortBirthDate)) {
            throw IllegalArgumentException("비밀번호에 생년월일(8자리 또는 6자리)을 포함할 수 없습니다.")
        }
    }
}
