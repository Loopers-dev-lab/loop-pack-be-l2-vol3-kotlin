package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.time.ZoneId
import java.time.ZonedDateTime

class UserModelTest {

    private val defaultUsername = "username"
    private val defaultPassword = "password1234!"
    private val defaultName = "안유진"
    private val defaultEmail = "email@loopers.com"
    private val defaultBirthDate = ZonedDateTime.of(1995, 5, 29, 21, 40, 0, 0, ZoneId.of("Asia/Seoul"))

    private fun createUserModel(
        username: String = defaultUsername,
        password: String = defaultPassword,
        name: String = defaultName,
        email: String = defaultEmail,
        birthDate: ZonedDateTime = defaultBirthDate,
    ) = UserModel(
        username = username,
        password = password,
        name = name,
        email = email,
        birthDate = birthDate,
    )

    @DisplayName("생성")
    @Nested
    inner class Create {

        @DisplayName("유효한 파라미터가 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsUserModel_whenValidParametersAreProvided() {
            // act
            val userModel = createUserModel()

            // assert
            assertAll(
                { assertThat(userModel.id).isNotNull() },
                { assertThat(userModel.username).isEqualTo(defaultUsername) },
                { assertThat(userModel.password).isEqualTo(defaultPassword) },
                { assertThat(userModel.name).isEqualTo(defaultName) },
                { assertThat(userModel.email).isEqualTo(defaultEmail) },
                { assertThat(userModel.birthDate).isEqualTo(defaultBirthDate) },
            )
        }

        @DisplayName("비밀번호 검증 - ")
        @Nested
        inner class PasswordValidation {

            @DisplayName("비어있을 때, BAD_REQUEST 예외가 발생한다.")
            @Test
            fun throwsBadRequestException_whenPasswordIsBlank() {
                // act
                val result = assertThrows<CoreException> {
                    createUserModel(password = "   ")
                }

                // assert
                assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            }

            @DisplayName("8자 미만 또는 16자 초과일 때, BAD_REQUEST 예외가 발생한다.")
            @Test
            fun throwsBadRequestException_whenPasswordLengthIsInvalid() {
                // act
                val result = assertThrows<CoreException> {
                    createUserModel(password = "pass1!")
                }

                // assert
                assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            }

            @DisplayName("영문자 또는 숫자 또는 특수문자로 이루어져있지 않으면, BAD_REQUEST 예외가 발생한다.")
            @Test
            fun throwsBadRequestException_whenPasswordContainsInvalidCharacters() {
                // act
                val result = assertThrows<CoreException> {
                    createUserModel(password = "패스워드12345678")
                }

                // assert
                assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            }

            @DisplayName("생년월일이 포함되어 있으면, BAD_REQUEST 예외가 발생한다.")
            @Test
            fun throwsBadRequestException_whenPasswordContainsBirthDate() {
                // act
                val result = assertThrows<CoreException> {
                    createUserModel(password = "pass19950529!")
                }

                // assert
                assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            }
        }

        @DisplayName("비밀번호 이외 파라미터 검증 - ")
        @Nested
        inner class ParameterValidation {

            @DisplayName("아이디가 비어있으면, BAD_REQUEST 예외가 발생한다.")
            @Test
            fun throwsBadRequestException_whenUsernameIsBlank() {
                // act
                val result = assertThrows<CoreException> {
                    createUserModel(username = "   ")
                }

                // assert
                assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            }

            @DisplayName("아이디가 영문과 숫자로 구성되지 않으면, BAD_REQUEST 예외가 발생한다.")
            @Test
            fun throwsBadRequestException_whenUsernameContainsInvalidCharacters() {
                // act
                val result = assertThrows<CoreException> {
                    createUserModel(username = "유저이름!@#")
                }

                // assert
                assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            }

            @DisplayName("이름이 비어있으면, BAD_REQUEST 예외가 발생한다.")
            @Test
            fun throwsBadRequestException_whenNameIsBlank() {
                // act
                val result = assertThrows<CoreException> {
                    createUserModel(name = "   ")
                }

                // assert
                assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            }

            @DisplayName("이메일이 비어있으면, BAD_REQUEST 예외가 발생한다.")
            @Test
            fun throwsBadRequestException_whenEmailIsBlank() {
                // act
                val result = assertThrows<CoreException> {
                    createUserModel(email = "   ")
                }

                // assert
                assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            }

            @DisplayName("이메일 형식이 올바르지 않으면, BAD_REQUEST 예외가 발생한다.")
            @Test
            fun throwsBadRequestException_whenEmailFormatIsInvalid() {
                // act
                val result = assertThrows<CoreException> {
                    createUserModel(email = "invalid-email")
                }

                // assert
                assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            }

            @DisplayName("생년월일이 현재 시점 이후이면, BAD_REQUEST 예외가 발생한다.")
            @Test
            fun throwsBadRequestException_whenBirthDateIsInTheFuture() {
                // act
                val result = assertThrows<CoreException> {
                    createUserModel(birthDate = ZonedDateTime.now().plusYears(1))
                }

                // assert
                assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            }
        }
    }

    @DisplayName("비밀번호 수정")
    @Nested
    inner class UpdatePassword {

        @DisplayName("유효한 새 비밀번호가 주어지면, 정상적으로 갱신된다.")
        @Test
        fun updatesPassword_whenNewPasswordIsValid() {
            // arrange
            val userModel = createUserModel()
            val newPassword = "newPassword1!"

            // act
            userModel.updatePassword(newPassword)

            // assert
            assertThat(userModel.password).isEqualTo(newPassword)
        }
    }
}
