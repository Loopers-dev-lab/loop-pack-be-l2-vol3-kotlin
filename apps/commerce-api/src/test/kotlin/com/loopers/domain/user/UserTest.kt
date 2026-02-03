package com.loopers.domain.user

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class UserTest {

    @Test
    fun `요구조건을 만족하면 통과한다` () {
        val loginId = LoginId("test1234")
        val password = Password("test1234!@#$")
        val name = Name("loopers")
        val birthDate = BirthDate("2000-01-01")
        val email = Email("test1234@loopers.com")

        assertDoesNotThrow {
            User(loginId, password, name, birthDate, email)
        }
    }

    @Test
    fun `비밀번호 내 생년월일(8자리)이 포함된 경우 실패한다` () {
        val loginId = LoginId("test1234")
        val password = Password("test20000101!@#$")
        val name = Name("loopers")
        val birthDate = BirthDate("2000-01-01")
        val email = Email("test1234@loopers.com")

        assertThrows<IllegalArgumentException> {
            User(loginId, password, name, birthDate, email)
        }
    }

    @Test
    fun `비밀번호 내 생년월일(6자리)이 포함된 경우 실패한다` () {
        val loginId = LoginId("test1234")
        val password = Password("test000101!@#$")
        val name = Name("loopers")
        val birthDate = BirthDate("2000-01-01")
        val email = Email("test1234@loopers.com")

        assertThrows<IllegalArgumentException> {
            User(loginId, password, name, birthDate, email)
        }
    }
}
