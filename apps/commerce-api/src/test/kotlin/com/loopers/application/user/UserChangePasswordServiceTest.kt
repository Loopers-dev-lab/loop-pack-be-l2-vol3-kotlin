package com.loopers.application.user

import com.loopers.application.user.model.UserChangePasswordCommand
import com.loopers.domain.user.EncodedPassword
import com.loopers.domain.user.RawPassword
import com.loopers.domain.user.User
import com.loopers.domain.user.UserPasswordHasher
import com.loopers.domain.user.UserRepository
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

@DisplayName("UserChangePasswordService")
class UserChangePasswordServiceTest {
    private val userRepository: UserRepository = mock()
    private val passwordHasher: UserPasswordHasher = mock()
    private val service = UserChangePasswordService(userRepository, passwordHasher)

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
    @DisplayName("비밀번호 변경 성공")
    inner class WhenChangePasswordSuccess {
        @Test
        @DisplayName("새 비밀번호가 인코딩되어 저장된다")
        fun changePassword_success_savesEncodedPassword() {
            // arrange
            val user = existingUser()
            given(userRepository.findByLoginId("testuser1")).willReturn(user)
            given(passwordHasher.matches(RawPassword("Password1!"), EncodedPassword("encoded_Password1!")))
                .willReturn(true)
            given(passwordHasher.matches(RawPassword("NewPassword1!"), EncodedPassword("encoded_Password1!")))
                .willReturn(false)
            given(passwordHasher.encode(RawPassword("NewPassword1!")))
                .willReturn(EncodedPassword("encoded_NewPassword1!"))
            given(userRepository.save(any())).willAnswer { it.arguments[0] as User }

            // act
            service.changePassword("testuser1", "Password1!", UserChangePasswordCommand("Password1!", "NewPassword1!"))

            // assert
            then(userRepository).should().save(
                check { saved ->
                    assertThat(saved.password.value).isEqualTo("encoded_NewPassword1!")
                },
            )
        }
    }

    @Nested
    @DisplayName("헤더 비밀번호 불일치 시 인증 실패한다")
    inner class WhenHeaderPasswordMismatch {
        @Test
        @DisplayName("CoreException(UNAUTHORIZED)을 던지고 save를 호출하지 않는다")
        fun changePassword_wrongHeaderPassword_throwsUnauthorized() {
            // arrange
            val user = existingUser()
            given(userRepository.findByLoginId("testuser1")).willReturn(user)
            given(passwordHasher.matches(RawPassword("WrongPassword1!"), EncodedPassword("encoded_Password1!")))
                .willReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                service.changePassword(
                    "testuser1",
                    "WrongPassword1!",
                    UserChangePasswordCommand("Password1!", "NewPassword1!"),
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
            then(userRepository).should(never()).save(any())
        }
    }

    @Nested
    @DisplayName("도메인 검증 실패 시 예외가 전파된다")
    inner class WhenDomainValidationFails {
        @Test
        @DisplayName("현재 비밀번호 불일치 시 CoreException(USER_INVALID_PASSWORD)을 전파하고 save를 호출하지 않는다")
        fun changePassword_wrongCurrentPassword_propagatesExceptionAndDoesNotSave() {
            // arrange
            val user = existingUser()
            given(userRepository.findByLoginId("testuser1")).willReturn(user)
            given(passwordHasher.matches(RawPassword("Password1!"), EncodedPassword("encoded_Password1!")))
                .willReturn(true)
            given(passwordHasher.matches(RawPassword("WrongCurrent1!"), EncodedPassword("encoded_Password1!")))
                .willReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                service.changePassword(
                    "testuser1",
                    "Password1!",
                    UserChangePasswordCommand("WrongCurrent1!", "NewPassword1!"),
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.USER_INVALID_PASSWORD)
            then(userRepository).should(never()).save(any())
        }

        @Test
        @DisplayName("동일 비밀번호 시 CoreException(USER_INVALID_PASSWORD)을 전파하고 save를 호출하지 않는다")
        fun changePassword_samePassword_propagatesExceptionAndDoesNotSave() {
            // arrange
            val user = existingUser()
            given(userRepository.findByLoginId("testuser1")).willReturn(user)
            given(passwordHasher.matches(RawPassword("Password1!"), EncodedPassword("encoded_Password1!")))
                .willReturn(true)

            // act
            val exception = assertThrows<CoreException> {
                service.changePassword(
                    "testuser1",
                    "Password1!",
                    UserChangePasswordCommand("Password1!", "Password1!"),
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.USER_INVALID_PASSWORD)
            then(userRepository).should(never()).save(any())
        }
    }
}
