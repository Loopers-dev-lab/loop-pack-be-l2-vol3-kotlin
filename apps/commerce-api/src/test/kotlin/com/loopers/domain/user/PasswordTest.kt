package com.loopers.domain.user

import com.loopers.domain.user.fixture.TestPasswordEncoder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class PasswordTest {

    private val encoder = TestPasswordEncoder()

    @Test
    fun `올바른 비밀번호의 경우 Password 생성이 성공해야 한다`() {
        val password = Password.create("Password1!", encoder)
        assertThat(password.toEncodedString()).isNotBlank()
    }

    @Test
    fun `8자 미만의 경우 Password 생성이 실패해야 한다`() {
        assertThatThrownBy { Password.create("Pass1!", encoder) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `16자 초과의 경우 Password 생성이 실패해야 한다`() {
        assertThatThrownBy { Password.create("Password123456789!", encoder) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `허용되지 않은 문자 포함의 경우 Password 생성이 실패해야 한다`() {
        assertThatThrownBy { Password.create("Password1!한글", encoder) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `matches 호출시 올바른 비밀번호면 true를 반환해야 한다`() {
        val password = Password.create("Password1!", encoder)
        assertThat(password.matches("Password1!", encoder)).isTrue()
    }

    @Test
    fun `matches 호출시 틀린 비밀번호면 false를 반환해야 한다`() {
        val password = Password.create("Password1!", encoder)
        assertThat(password.matches("WrongPass1!", encoder)).isFalse()
    }

    @Test
    fun `toString 호출시 비밀번호가 마스킹되어야 한다`() {
        val password = Password.create("Password1!", encoder)
        assertThat(password.toString()).isEqualTo("Password(****)")
    }
}
