package com.loopers.domain.user

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class UserModelTest {

    private val encryptor = PasswordEncryptor()

    @Test
    fun `EncryptedPassword로 UserModel을 생성하면 성공한다`() {
        val loginId = LoginId("test1234")
        val encryptedPassword = encryptor.encrypt("test1234")
        val name = Name("loopers")
        val birthDate = BirthDate("2000-01-01")
        val email = Email("test1234@loopers.com")

        assertDoesNotThrow {
            UserModel(loginId, encryptedPassword, name, birthDate, email)
        }
    }
}
