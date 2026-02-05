package com.loopers.domain.user

import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class UserServiceTest @Autowired constructor(
    private val userService: UserService,
    private val userJpaRepository: UserJpaRepository,
    private val passwordEncryptor: PasswordEncryptor,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("사용자 생성 시,")
    @Nested
    inner class CreateUser {

        @Test
        fun `유효한 정보로 생성하면 성공한다`() {
            val loginId = LoginId("test1234")
            val password = Password("test1234!@#$")
            val name = Name("loopers")
            val birthDate = BirthDate("2000-01-01")
            val email = Email("test1234@loopers.com")

            val user = userService.createUser(loginId, password, name, birthDate, email)

            assertAll(
                { assertThat(user.id).isGreaterThan(0) },
                { assertThat(user.loginId).isEqualTo(loginId) },
                { assertThat(user.password).isNotEqualTo(password.value) },
                { assertThat(passwordEncryptor.matches(password.value, user.password)).isTrue() },
                { assertThat(user.name).isEqualTo(name) },
                { assertThat(user.birthDate).isEqualTo(birthDate) },
                { assertThat(user.email).isEqualTo(email) },
            )
        }

        @Test
        fun `비밀번호에 생년월일(8자리)이 포함되면 실패한다`() {
            val loginId = LoginId("test1234")
            val password = Password("test20000101!@#$")
            val name = Name("loopers")
            val birthDate = BirthDate("2000-01-01")
            val email = Email("test1234@loopers.com")

            val exception = assertThrows<CoreException> {
                userService.createUser(loginId, password, name, birthDate, email)
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        fun `비밀번호에 생년월일(6자리)이 포함되면 실패한다`() {
            val loginId = LoginId("test1234")
            val password = Password("test000101!@#$")
            val name = Name("loopers")
            val birthDate = BirthDate("2000-01-01")
            val email = Email("test1234@loopers.com")

            val exception = assertThrows<CoreException> {
                userService.createUser(loginId, password, name, birthDate, email)
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("사용자 조회 시,")
    @Nested
    inner class GetUser {

        @Test
        fun `존재하는 LoginId로 조회하면 성공한다`() {
            val user = userService.createUser(
                LoginId("test1234"),
                Password("test1234!@#$"),
                Name("loopers"),
                BirthDate("2000-01-01"),
                Email("test1234@loopers.com"),
            )

            val found = userService.getUserByLoginId(user.loginId)

            found?.let { assertThat(it.loginId) }?.isEqualTo(user.loginId)
        }

        @Test
        fun `존재하지 않는 LoginId로 조회하면 NOT_FOUND 예외가 발생한다`() {
            val exception = assertThrows<CoreException> {
                userService.getUserByLoginId(LoginId("test1234"))
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("비밀번호 수정 시,")
    @Nested
    inner class UpdatePassword {

        @Test
        fun `유효한 비밀번호로 수정하면 성공한다`() {
            val user = userService.createUser(
                LoginId("test1234"),
                Password("test1234!@#$"),
                Name("loopers"),
                BirthDate("2000-01-01"),
                Email("test1234@loopers.com"),
            )
            val newPassword = Password("newpass1234!@#$")

            userService.updatePassword(user.loginId, newPassword, user.birthDate)

            val updated = userService.getUserByLoginId(user.loginId)
            updated?.let { assertThat(passwordEncryptor.matches(newPassword.value, it.password)) }?.isTrue()
        }

        @Test
        fun `비밀번호에 생년월일이 포함되면 실패한다`() {
            val user = userService.createUser(
                LoginId("test1234"),
                Password("test1234!@#$"),
                Name("loopers"),
                BirthDate("2000-01-01"),
                Email("test1234@loopers.com"),
            )
            val newPassword = Password("test20000101!@#$")

            val exception = assertThrows<CoreException> {
                userService.updatePassword(user.loginId, newPassword, user.birthDate)
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
