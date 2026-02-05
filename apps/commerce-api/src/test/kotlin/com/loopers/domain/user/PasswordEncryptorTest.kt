package com.loopers.domain.user

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PasswordEncryptorTest {

    private val passwordEncryptor = PasswordEncryptor()

    @Test
    fun `비밀번호를 암호화에 성공한다`() {
        val rawPassword = "Test123!@#"
        val encrypted = passwordEncryptor.encrypt(rawPassword)

        assertThat(encrypted).isNotEqualTo(rawPassword)
        assertThat(encrypted).isNotEmpty()
    }

    @Test
    fun `원본 비밀번호와 암호화된 비밀번호가 일치하는지 검증할 수 있다`() {
        val rawPassword = "Test123!@#"
        val encrypted = passwordEncryptor.encrypt(rawPassword)

        assertThat(passwordEncryptor.matches(rawPassword, encrypted)).isTrue()
        assertThat(passwordEncryptor.matches("WrongPassword!", encrypted)).isFalse()
    }
}
