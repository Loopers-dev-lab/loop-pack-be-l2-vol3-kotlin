package com.loopers.user.application

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.user.application.model.UserSignUpCommand
import com.loopers.user.domain.User
import com.loopers.user.domain.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDate

@DisplayName("UserService 회원가입")
class UserServiceTest {
    private val userRepository: UserRepository = mock()
    private val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()
    private val userService = UserService(userRepository, passwordEncoder)

    private fun createCommand(
        loginId: String = "testuser1",
        password: String = "Password1!",
        name: String = "홍길동",
        birthDate: LocalDate = LocalDate.of(1990, 1, 1),
        email: String = "test@example.com",
    ): UserSignUpCommand = UserSignUpCommand(loginId, password, name, birthDate, email)

    @Nested
    @DisplayName("성공")
    inner class Success {
        @Test
        @DisplayName("회원가입 성공 - UserInfo 반환")
        fun signUp_success_returnsUserInfo() {
            // arrange
            val command = createCommand()
            given(userRepository.existsByLoginId(command.loginId)).willReturn(false)
            given(userRepository.save(any())).willAnswer { it.arguments[0] as User }

            // act
            val result = userService.signUp(command)

            // assert
            assertThat(result.loginId).isEqualTo("testuser1")
        }

        @Test
        @DisplayName("회원가입 성공 - 비밀번호가 BCrypt로 암호화됨")
        fun signUp_success_passwordIsEncrypted() {
            // arrange
            val command = createCommand()
            given(userRepository.existsByLoginId(command.loginId)).willReturn(false)
            given(userRepository.save(any())).willAnswer { it.arguments[0] as User }

            // act
            userService.signUp(command)

            // assert
            then(userRepository).should().save(
                org.mockito.kotlin.check { user ->
                    assertThat(user.password).isNotEqualTo("Password1!")
                    assertThat(passwordEncoder.matches("Password1!", user.password)).isTrue
                },
            )
        }

        @Test
        @DisplayName("회원가입 성공 - Repository.save() 호출됨")
        fun signUp_success_callsRepositorySave() {
            // arrange
            val command = createCommand()
            given(userRepository.existsByLoginId(command.loginId)).willReturn(false)
            given(userRepository.save(any())).willAnswer { it.arguments[0] as User }

            // act
            userService.signUp(command)

            // assert
            then(userRepository).should().save(any())
        }
    }

    @Nested
    @DisplayName("실패")
    inner class Failure {
        @Test
        @DisplayName("회원가입 실패 - 중복 loginId - CoreException(USER_DUPLICATE_LOGIN_ID)")
        fun signUp_duplicateLoginId_throwsException() {
            // arrange
            val command = createCommand()
            given(userRepository.existsByLoginId(command.loginId)).willReturn(true)

            // act
            val exception = assertThrows<CoreException> {
                userService.signUp(command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.USER_DUPLICATE_LOGIN_ID)
        }
    }
}
