package com.loopers.domain.user

import com.loopers.domain.user.fixture.TestPasswordEncoder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class UserTest {

    private val encoder = TestPasswordEncoder()
    private val birthDate = BirthDate.from(BIRTH_DATE)

    @Test
    fun `register로 생성한 User의 id는 null이어야 한다`() {
        val user = createUser()
        assertThat(user.id).isNull()
    }

    @Test
    fun `reconstitute로 생성한 User는 id를 가져야 한다`() {
        val user = User.reconstitute(
            id = 1L,
            loginId = LoginId(LOGIN_ID),
            password = Password.create(RAW_PASSWORD, encoder),
            name = Name(NAME),
            birthDate = birthDate,
            email = Email(EMAIL),
            gender = GenderType.MALE,
        )
        assertThat(user.id).isEqualTo(1L)
    }

    @Test
    fun `올바른 비밀번호의 경우 authenticate가 true를 반환해야 한다`() {
        val user = createUser()
        assertThat(user.authenticate(RAW_PASSWORD, encoder)).isTrue()
    }

    @Test
    fun `틀린 비밀번호의 경우 authenticate가 false를 반환해야 한다`() {
        val user = createUser()
        assertThat(user.authenticate(WRONG_PASSWORD, encoder)).isFalse()
    }

    @Test
    fun `기존 비밀번호 불일치의 경우 changePassword가 실패해야 한다`() {
        val user = createUser()
        assertThatThrownBy { user.changePassword(WRONG_PASSWORD, NEW_PASSWORD, encoder) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `새 비밀번호가 기존과 동일한 경우 changePassword가 실패해야 한다`() {
        val user = createUser()
        assertThatThrownBy { user.changePassword(RAW_PASSWORD, RAW_PASSWORD, encoder) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `정상적인 경우 changePassword가 새 User를 반환해야 한다`() {
        val user = createUser()
        val newUser = user.changePassword(RAW_PASSWORD, NEW_PASSWORD, encoder)

        assertThat(newUser).isNotSameAs(user)
        assertThat(newUser.authenticate(NEW_PASSWORD, encoder)).isTrue()
    }

    @Test
    fun `비밀번호에 생년월일이 포함된 경우 register가 실패해야 한다`() {
        assertThatThrownBy {
            User.register(
                loginId = LoginId(LOGIN_ID),
                rawPassword = "Pass19930401!",
                name = Name(NAME),
                birthDate = birthDate,
                email = Email(EMAIL),
                gender = GenderType.MALE,
                encoder = encoder,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("생년월일")
    }

    @Test
    fun `changePassword에서 새 비밀번호에 생년월일이 포함된 경우 실패해야 한다`() {
        val user = createUser()
        assertThatThrownBy { user.changePassword(RAW_PASSWORD, "New19930401!", encoder) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("생년월일")
    }

    private fun createUser(): User = User.register(
        loginId = LoginId(LOGIN_ID),
        rawPassword = RAW_PASSWORD,
        name = Name(NAME),
        birthDate = birthDate,
        email = Email(EMAIL),
        gender = GenderType.MALE,
        encoder = encoder,
    )

    companion object {
        private const val LOGIN_ID = "testuser"
        private const val RAW_PASSWORD = "Password1!"
        private const val WRONG_PASSWORD = "WrongPass1!"
        private const val NEW_PASSWORD = "NewPass123!"
        private const val NAME = "신형기"
        private const val EMAIL = "tkaqkeldk99@gmail.com"
        private const val BIRTH_DATE = "1993-04-01"
    }
}
