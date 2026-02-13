package com.loopers.domain.user

import java.time.LocalDate

object UserTestFixture {

    const val DEFAULT_LOGIN_ID = "testuser1"
    const val DEFAULT_PASSWORD = "Password1!"
    const val DEFAULT_NAME = "홍길동"
    val DEFAULT_BIRTH_DATE: LocalDate = LocalDate.of(1990, 1, 15)
    const val DEFAULT_EMAIL = "test@example.com"

    fun createUser(
        loginId: String = DEFAULT_LOGIN_ID,
        password: String = DEFAULT_PASSWORD,
        name: String = DEFAULT_NAME,
        birthDate: LocalDate = DEFAULT_BIRTH_DATE,
        email: String = DEFAULT_EMAIL,
    ): User = User(loginId, password, name, birthDate, email)
}
