package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class UserTest {

    private fun createUser(
        loginId: String = "testuser1",
        password: String = "hashedPassword",
        name: String = "홍길동",
        birthday: LocalDate = LocalDate.of(1999, 1, 1),
        email: String = "test@email.com",
    ): User = User(
        loginId = loginId,
        password = password,
        name = name,
        birthday = birthday,
        email = email,
    )

    @Nested
    inner class `유저를 생성할 때` {

        @Test
        fun `모든 정보가 올바르면 정상적으로 생성된다`() {
            // arrange & act
            val user = createUser()

            // assert
            assertAll(
                { assertThat(user.loginId).isEqualTo("testuser1") },
                { assertThat(user.name).isEqualTo("홍길동") },
                { assertThat(user.birthday).isEqualTo(LocalDate.of(1999, 1, 1)) },
                { assertThat(user.email).isEqualTo("test@email.com") },
            )
        }

        @Test
        fun `로그인ID에 한글이 포함되면 BAD_REQUEST 예외가 발생한다`() {
            // arrange & act
            val result = assertThrows<CoreException> {
                createUser(loginId = "홍길동")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        fun `로그인ID에 특수문자가 포함되면 BAD_REQUEST 예외가 발생한다`() {
            // arrange & act
            val result = assertThrows<CoreException> {
                createUser(loginId = "test@user")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        fun `이름이 빈칸이면 BAD_REQUEST 예외가 발생한다`() {
            // arrange & act
            val result = assertThrows<CoreException> {
                createUser(name = "   ")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        fun `이메일 형식이 올바르지 않으면 BAD_REQUEST 예외가 발생한다`() {
            // arrange & act
            val result = assertThrows<CoreException> {
                createUser(email = "invalid-email")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        fun `생년월일이 미래이면 BAD_REQUEST 예외가 발생한다`() {
            // arrange & act
            val result = assertThrows<CoreException> {
                createUser(birthday = LocalDate.now().plusDays(1))
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Nested
    inner class `이름 마스킹 시` {

        @Test
        fun `3글자 이름이면 마지막 글자가 마스킹된다`() {
            // arrange
            val user = createUser(name = "홍길동")

            // act
            val result = user.maskedName()

            // assert
            assertThat(result).isEqualTo("홍길*")
        }

        @Test
        fun `2글자 이름이면 마지막 글자가 마스킹된다`() {
            // arrange
            val user = createUser(name = "홍길")

            // act
            val result = user.maskedName()

            // assert
            assertThat(result).isEqualTo("홍*")
        }

        @Test
        fun `1글자 이름이면 전체가 마스킹된다`() {
            // arrange
            val user = createUser(name = "홍")

            // act
            val result = user.maskedName()

            // assert
            assertThat(result).isEqualTo("*")
        }
    }
}
