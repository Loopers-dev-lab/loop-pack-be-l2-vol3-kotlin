package com.loopers.domain.user

import com.loopers.application.user.model.UserSignUpCommand
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import java.time.LocalDate

@DisplayName("UserService (domain)")
class UserServiceTest {
    private val userRepository: UserRepository = mock()
    private val passwordHasher: UserPasswordHasher = mock()
    private val userService = UserService(userRepository, passwordHasher)

    private fun createSignUpCommand(
        loginId: String = "testuser1",
        password: String = "Password1!",
        name: String = "홍길동",
        birthDate: LocalDate = LocalDate.of(1990, 1, 1),
        email: String = "test@example.com",
    ): UserSignUpCommand = UserSignUpCommand(loginId, password, name, birthDate, email)

    private fun existingUser(
        password: String = "encoded_Password1!",
        birthDate: LocalDate = LocalDate.of(1990, 1, 1),
    ): User = User.retrieve(
        id = 1L,
        loginId = "testuser1",
        password = password,
        name = "홍길동",
        birthDate = birthDate,
        email = "test@example.com",
    )

    @Nested
    @DisplayName("회원가입")
    inner class Register {
        @Test
        @DisplayName("회원가입 성공 - 저장된 User 반환")
        fun register_success_returnsUser() {
            // arrange
            val command = createSignUpCommand()
            given(userRepository.existsByLoginId(command.loginId)).willReturn(false)
            given(passwordHasher.encode(command.password)).willReturn("encoded_Password1!")
            given(userRepository.save(any())).willAnswer { it.arguments[0] as User }

            // act
            val result = userService.register(command)

            // assert
            assertThat(result.loginId).isEqualTo("testuser1")
        }

        @Test
        @DisplayName("회원가입 성공 - 비밀번호가 인코딩되어 저장된다")
        fun register_success_passwordIsEncoded() {
            // arrange
            val command = createSignUpCommand()
            given(userRepository.existsByLoginId(command.loginId)).willReturn(false)
            given(passwordHasher.encode(command.password)).willReturn("encoded_Password1!")
            given(userRepository.save(any())).willAnswer { it.arguments[0] as User }

            // act
            userService.register(command)

            // assert
            then(userRepository).should().save(
                check { user ->
                    assertThat(user.password).isEqualTo("encoded_Password1!")
                },
            )
        }

        @Test
        @DisplayName("회원가입 성공 - Repository.save() 호출됨")
        fun register_success_callsRepositorySave() {
            // arrange
            val command = createSignUpCommand()
            given(userRepository.existsByLoginId(command.loginId)).willReturn(false)
            given(passwordHasher.encode(command.password)).willReturn("encoded_Password1!")
            given(userRepository.save(any())).willAnswer { it.arguments[0] as User }

            // act
            userService.register(command)

            // assert
            then(userRepository).should().save(any())
        }

        @Test
        @DisplayName("회원가입 실패 - 중복 loginId - CoreException(USER_DUPLICATE_LOGIN_ID)")
        fun register_duplicateLoginId_throwsException() {
            // arrange
            val command = createSignUpCommand()
            given(userRepository.existsByLoginId(command.loginId)).willReturn(true)

            // act
            val exception = assertThrows<CoreException> {
                userService.register(command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.USER_DUPLICATE_LOGIN_ID)
        }
    }

    @Nested
    @DisplayName("내 정보 조회")
    inner class FindByCredentials {
        @Test
        @DisplayName("유효한 인증 정보로 조회 시 User를 반환한다")
        fun findByCredentials_success_returnsUser() {
            // arrange
            val user = existingUser()
            given(userRepository.findByLoginId("testuser1")).willReturn(user)
            given(passwordHasher.matches("Password1!", "encoded_Password1!")).willReturn(true)

            // act
            val result = userService.findByCredentials("testuser1", "Password1!")

            // assert
            assertThat(result.loginId).isEqualTo("testuser1")
        }

        @Test
        @DisplayName("존재하지 않는 loginId로 조회 시 CoreException(UNAUTHORIZED)")
        fun findByCredentials_invalidLoginId_throwsException() {
            // arrange
            given(userRepository.findByLoginId("nonexistent")).willReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                userService.findByCredentials("nonexistent", "Password1!")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }

        @Test
        @DisplayName("비밀번호 불일치 시 CoreException(UNAUTHORIZED)")
        fun findByCredentials_wrongPassword_throwsException() {
            // arrange
            val user = existingUser()
            given(userRepository.findByLoginId("testuser1")).willReturn(user)
            given(passwordHasher.matches("WrongPassword1!", "encoded_Password1!")).willReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                userService.findByCredentials("testuser1", "WrongPassword1!")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }
    }

    @Nested
    @DisplayName("비밀번호 수정")
    inner class ChangePassword {
        @Test
        @DisplayName("유효한 요청으로 비밀번호를 변경하면 새 비밀번호가 인코딩되어 저장된다")
        fun changePassword_success_savesEncodedPassword() {
            // arrange
            val user = existingUser()
            given(userRepository.findByLoginId("testuser1")).willReturn(user)
            given(passwordHasher.matches("Password1!", "encoded_Password1!")).willReturn(true)
            given(passwordHasher.matches("NewPassword1!", "encoded_Password1!")).willReturn(false)
            given(passwordHasher.encode("NewPassword1!")).willReturn("encoded_NewPassword1!")
            given(userRepository.save(any())).willAnswer { it.arguments[0] as User }

            // act
            userService.changePassword("testuser1", "Password1!", "Password1!", "NewPassword1!")

            // assert
            then(userRepository).should().save(
                check { saved ->
                    assertThat(saved.password).isEqualTo("encoded_NewPassword1!")
                },
            )
        }

        @Test
        @DisplayName("헤더 비밀번호 불일치 시 CoreException(UNAUTHORIZED)")
        fun changePassword_wrongHeaderPassword_throwsUnauthorized() {
            // arrange
            val user = existingUser()
            given(userRepository.findByLoginId("testuser1")).willReturn(user)
            given(passwordHasher.matches("WrongPassword1!", "encoded_Password1!")).willReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword("testuser1", "WrongPassword1!", "Password1!", "NewPassword1!")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }

        @Test
        @DisplayName("도메인에서 현재 비밀번호 불일치 예외가 발생하면 전파하고 save를 호출하지 않는다")
        fun changePassword_wrongCurrentPassword_propagatesExceptionAndDoesNotSave() {
            // arrange
            val user = existingUser()
            given(userRepository.findByLoginId("testuser1")).willReturn(user)
            given(passwordHasher.matches("Password1!", "encoded_Password1!")).willReturn(true)
            given(passwordHasher.matches("WrongCurrent1!", "encoded_Password1!")).willReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword("testuser1", "Password1!", "WrongCurrent1!", "NewPassword1!")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.USER_INVALID_PASSWORD)
            then(userRepository).should(never()).save(any())
        }

        @Test
        @DisplayName("도메인에서 동일 비밀번호 예외가 발생하면 전파하고 save를 호출하지 않는다")
        fun changePassword_samePassword_propagatesExceptionAndDoesNotSave() {
            // arrange
            val user = existingUser()
            given(userRepository.findByLoginId("testuser1")).willReturn(user)
            given(passwordHasher.matches("Password1!", "encoded_Password1!")).willReturn(true)

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword("testuser1", "Password1!", "Password1!", "Password1!")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.USER_INVALID_PASSWORD)
            then(userRepository).should(never()).save(any())
        }
    }
}
