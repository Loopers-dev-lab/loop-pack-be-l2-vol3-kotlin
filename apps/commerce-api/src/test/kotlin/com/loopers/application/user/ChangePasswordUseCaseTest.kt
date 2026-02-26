package com.loopers.application.user

import com.loopers.domain.user.FakeUserRepository
import com.loopers.domain.user.UserTestFixture
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ChangePasswordUseCaseTest {

    private lateinit var userRepository: FakeUserRepository
    private lateinit var changePasswordUseCase: ChangePasswordUseCase

    @BeforeEach
    fun setUp() {
        userRepository = FakeUserRepository()
        changePasswordUseCase = ChangePasswordUseCase(userRepository)
    }

    @Nested
    @DisplayName("execute 시")
    inner class Execute {

        @Test
        @DisplayName("유효한 요청이면 비밀번호가 변경된다")
        fun execute_withValidRequest_changesPassword() {
            // arrange
            val user = userRepository.save(UserTestFixture.createUser())

            // act
            changePasswordUseCase.execute(
                userId = user.id.value,
                currentPassword = UserTestFixture.DEFAULT_PASSWORD,
                newPassword = "NewPass12!",
            )

            // assert
            val updated = userRepository.findById(user.id)!!
            assertThat(updated.verifyPassword("NewPass12!")).isTrue()
        }

        @Test
        @DisplayName("존재하지 않는 사용자이면 NOT_FOUND 예외가 발생한다")
        fun execute_userNotFound_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                changePasswordUseCase.execute(
                    userId = 999L,
                    currentPassword = UserTestFixture.DEFAULT_PASSWORD,
                    newPassword = "NewPass12!",
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
            assertThat(exception.message).isEqualTo("사용자를 찾을 수 없습니다.")
        }

        @Test
        @DisplayName("현재 비밀번호가 일치하지 않으면 BAD_REQUEST 예외가 발생한다")
        fun execute_wrongCurrentPassword_throwsBadRequest() {
            // arrange
            val user = userRepository.save(UserTestFixture.createUser())

            // act
            val exception = assertThrows<CoreException> {
                changePasswordUseCase.execute(
                    userId = user.id.value,
                    currentPassword = "WrongPass1!",
                    newPassword = "NewPass12!",
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("현재 비밀번호가 일치하지 않습니다.")
        }

        @Test
        @DisplayName("현재 비밀번호와 동일한 비밀번호로 변경하면 BAD_REQUEST 예외가 발생한다")
        fun execute_sameAsCurrent_throwsBadRequest() {
            // arrange
            val user = userRepository.save(UserTestFixture.createUser())

            // act
            val exception = assertThrows<CoreException> {
                changePasswordUseCase.execute(
                    userId = user.id.value,
                    currentPassword = UserTestFixture.DEFAULT_PASSWORD,
                    newPassword = UserTestFixture.DEFAULT_PASSWORD,
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("현재 비밀번호와 동일한 비밀번호는 사용할 수 없습니다.")
        }

        @Test
        @DisplayName("새 비밀번호가 8자 미만이면 BAD_REQUEST 예외가 발생한다")
        fun execute_tooShortNewPassword_throwsBadRequest() {
            // arrange
            val user = userRepository.save(UserTestFixture.createUser())

            // act
            val exception = assertThrows<CoreException> {
                changePasswordUseCase.execute(
                    userId = user.id.value,
                    currentPassword = UserTestFixture.DEFAULT_PASSWORD,
                    newPassword = "Short1!",
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("새 비밀번호가 16자 초과하면 BAD_REQUEST 예외가 발생한다")
        fun execute_tooLongNewPassword_throwsBadRequest() {
            // arrange
            val user = userRepository.save(UserTestFixture.createUser())

            // act
            val exception = assertThrows<CoreException> {
                changePasswordUseCase.execute(
                    userId = user.id.value,
                    currentPassword = UserTestFixture.DEFAULT_PASSWORD,
                    newPassword = "Password12345678!",
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("새 비밀번호에 생년월일이 포함되면 BAD_REQUEST 예외가 발생한다")
        fun execute_newPasswordContainsBirthDate_throwsBadRequest() {
            // arrange
            val user = userRepository.save(UserTestFixture.createUser())

            // act
            val exception = assertThrows<CoreException> {
                changePasswordUseCase.execute(
                    userId = user.id.value,
                    currentPassword = UserTestFixture.DEFAULT_PASSWORD,
                    newPassword = "Pass19900115!",
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("비밀번호에 생년월일을 포함할 수 없습니다.")
        }

        @Test
        @DisplayName("새 비밀번호에 동일 문자가 3회 이상 연속되면 BAD_REQUEST 예외가 발생한다")
        fun execute_newPasswordHasConsecutiveChars_throwsBadRequest() {
            // arrange
            val user = userRepository.save(UserTestFixture.createUser())

            // act
            val exception = assertThrows<CoreException> {
                changePasswordUseCase.execute(
                    userId = user.id.value,
                    currentPassword = UserTestFixture.DEFAULT_PASSWORD,
                    newPassword = "Passsword1!",
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("동일 문자가 3회 이상 연속될 수 없습니다.")
        }
    }
}
