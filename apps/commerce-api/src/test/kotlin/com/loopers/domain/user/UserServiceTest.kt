package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDate

class UserServiceTest {

    private lateinit var userService: UserService
    private lateinit var userRepository: FakeUserRepository
    private val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()

    @BeforeEach
    fun setUp() {
        userRepository = FakeUserRepository()
        userService = UserService(userRepository, passwordEncoder)
    }

    @Nested
    inner class `회원가입 시` {

        @Test
        fun `올바른 정보로 가입하면 유저가 생성된다`() {
            // arrange
            val command = UserService.SignUpCommand(
                loginId = "testuser1",
                password = "Abcd1234!",
                name = "홍길동",
                birthday = LocalDate.of(1999, 1, 1),
                email = "test@email.com",
            )

            // act
            val user = userService.signUp(command)

            // assert
            assertThat(user.loginId).isEqualTo("testuser1")
            assertThat(user.name).isEqualTo("홍길동")
        }

        @Test
        fun `비밀번호가 암호화되어 저장된다`() {
            // arrange
            val rawPassword = "Abcd1234!"
            val command = UserService.SignUpCommand(
                loginId = "testuser1",
                password = rawPassword,
                name = "홍길동",
                birthday = LocalDate.of(1999, 1, 1),
                email = "test@email.com",
            )

            // act
            val user = userService.signUp(command)

            // assert
            assertThat(user.password).isNotEqualTo(rawPassword)
            assertThat(passwordEncoder.matches(rawPassword, user.password)).isTrue()
        }

        @Test
        fun `이미 존재하는 loginId로 가입하면 CONFLICT 예외가 발생한다`() {
            // arrange
            val command = UserService.SignUpCommand(
                loginId = "testuser1",
                password = "Abcd1234!",
                name = "홍길동",
                birthday = LocalDate.of(1999, 1, 1),
                email = "test@email.com",
            )
            userService.signUp(command) // 첫 번째 가입

            // act
            val result = assertThrows<CoreException> {
                userService.signUp(command) // 같은 loginId로 다시 가입
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @Test
        fun `비밀번호 규칙에 맞지 않으면 BAD_REQUEST 예외가 발생한다`() {
            // arrange
            val command = UserService.SignUpCommand(
                loginId = "testuser1",
                password = "short",
                name = "홍길동",
                birthday = LocalDate.of(1999, 1, 1),
                email = "test@email.com",
            )

            // act
            val result = assertThrows<CoreException> {
                userService.signUp(command)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Nested
    inner class `인증 시` {

        @Test
        fun `올바른 loginId와 password로 인증하면 유저 정보를 반환한다`() {
            // arrange
            val loginId = "testuser1"
            val password = "Abcd1234!"
            val signUpCommand = UserService.SignUpCommand(
                loginId = loginId,
                password = password,
                name = "홍길동",
                birthday = LocalDate.of(1999, 1, 1),
                email = "test@email.com",
            )
            userService.signUp(signUpCommand)

            // act
            val user = userService.authenticate(loginId, password)

            // assert
            assertThat(user.loginId).isEqualTo("testuser1")
            assertThat(user.name).isEqualTo("홍길동")
        }

        @Test
        fun `존재하지 않는 loginId로 인증하면 UNAUTHORIZED 예외가 발생한다`() {
            // arrange
            val nonExistentLoginId = "nonexistent"

            // act
            val result = assertThrows<CoreException> {
                userService.authenticate(nonExistentLoginId, "anyPassword")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }

        @Test
        fun `잘못된 password로 인증하면 UNAUTHORIZED 예외가 발생한다`() {
            // arrange
            val loginId = "testuser1"
            val signUpCommand = UserService.SignUpCommand(
                loginId = loginId,
                password = "Abcd1234!",
                name = "홍길동",
                birthday = LocalDate.of(1999, 1, 1),
                email = "test@email.com",
            )
            userService.signUp(signUpCommand)

            // act
            val result = assertThrows<CoreException> {
                userService.authenticate(loginId, "WrongPassword!")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }
    }

    @Nested
    inner class `비밀번호 변경 시` {

        @Test
        fun `인증된 유저가 새 비밀번호로 변경하면 성공한다`() {
            // arrange
            val loginId = "testuser1"
            val originalPassword = "Abcd1234!"
            val newPassword = "Newpass1!"
            val signUpCommand = UserService.SignUpCommand(
                loginId = loginId,
                password = originalPassword,
                name = "홍길동",
                birthday = LocalDate.of(1999, 1, 1),
                email = "test@email.com",
            )
            userService.signUp(signUpCommand)
            val user = userService.authenticate(loginId, originalPassword)

            val changePasswordCommand = UserService.ChangePasswordCommand(
                newPassword = newPassword,
            )

            // act
            userService.changePassword(user, changePasswordCommand)

            // assert
            val updatedUser = userService.authenticate(loginId, newPassword)
            assertThat(passwordEncoder.matches(newPassword, updatedUser.password)).isTrue()
        }

        @Test
        fun `새 비밀번호가 규칙에 맞지 않으면 BAD_REQUEST 예외가 발생한다`() {
            // arrange
            val loginId = "testuser1"
            val originalPassword = "Abcd1234!"
            val signUpCommand = UserService.SignUpCommand(
                loginId = loginId,
                password = originalPassword,
                name = "홍길동",
                birthday = LocalDate.of(1999, 1, 1),
                email = "test@email.com",
            )
            userService.signUp(signUpCommand)
            val user = userService.authenticate(loginId, originalPassword)

            val changePasswordCommand = UserService.ChangePasswordCommand(
                newPassword = "short",  // 규칙 위반
            )

            // act
            val result = assertThrows<CoreException> {
                userService.changePassword(user, changePasswordCommand)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
