package com.loopers.application.user

import com.loopers.application.user.model.UserSignUpCommand
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
import java.time.LocalDate

@DisplayName("UserSignUpUseCase")
class UserSignUpUseCaseTest {
    private val userRepository: UserRepository = mock()
    private val passwordHasher: UserPasswordHasher = mock()
    private val userSignUpService = UserSignUpUseCase(userRepository, passwordHasher)

    private val defaultBirthDate = LocalDate.of(1990, 1, 1)

    private fun command(loginId: String = "testuser1"): UserSignUpCommand =
        UserSignUpCommand(loginId, "Password1!", "홍길동", defaultBirthDate, "test@example.com")

    @Nested
    @DisplayName("회원가입 성공")
    inner class WhenSignUpSuccess {
        @Test
        @DisplayName("UserSignUpResult(loginId)를 반환한다")
        fun signUp_success_returnsUserSignUpResult() {
            // arrange
            given(userRepository.existsByLoginId("testuser1")).willReturn(false)
            given(passwordHasher.encode(RawPassword("Password1!")))
                .willReturn(EncodedPassword("encoded_Password1!"))
            given(userRepository.save(any())).willAnswer { it.arguments[0] as User }

            // act
            val result = userSignUpService.signUp(command())

            // assert
            assertThat(result.loginId).isEqualTo("testuser1")
        }

        @Test
        @DisplayName("비밀번호가 인코딩되어 저장된다")
        fun signUp_success_passwordIsEncoded() {
            // arrange
            given(userRepository.existsByLoginId("testuser1")).willReturn(false)
            given(passwordHasher.encode(RawPassword("Password1!")))
                .willReturn(EncodedPassword("encoded_Password1!"))
            given(userRepository.save(any())).willAnswer { it.arguments[0] as User }

            // act
            userSignUpService.signUp(command())

            // assert
            then(userRepository).should().save(
                check { user ->
                    assertThat(user.password.value).isEqualTo("encoded_Password1!")
                },
            )
        }

        @Test
        @DisplayName("Repository.save()가 호출된다")
        fun signUp_success_callsRepositorySave() {
            // arrange
            given(userRepository.existsByLoginId("testuser1")).willReturn(false)
            given(passwordHasher.encode(RawPassword("Password1!")))
                .willReturn(EncodedPassword("encoded_Password1!"))
            given(userRepository.save(any())).willAnswer { it.arguments[0] as User }

            // act
            userSignUpService.signUp(command())

            // assert
            then(userRepository).should().save(
                check { user ->
                    assertThat(user.loginId.value).isEqualTo("testuser1")
                },
            )
        }
    }

    @Nested
    @DisplayName("중복 loginId이면 회원가입이 실패한다")
    inner class WhenDuplicateLoginId {
        @Test
        @DisplayName("CoreException(USER_DUPLICATE_LOGIN_ID)을 던진다")
        fun signUp_duplicateLoginId_throwsException() {
            // arrange
            given(userRepository.existsByLoginId("testuser1")).willReturn(true)

            // act
            val exception = assertThrows<CoreException> {
                userSignUpService.signUp(command())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.USER_DUPLICATE_LOGIN_ID)
        }
    }
}
