package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import java.time.LocalDate

@DisplayName("User 도메인")
class UserTest {
    private val defaultBirthDate = LocalDate.of(1990, 1, 1)
    private val passwordHasher: UserPasswordHasher = mock()

    private fun registerUser(
        loginId: String = "testuser1",
        rawPassword: String = "Password1!",
        name: String = "홍길동",
        birthDate: LocalDate = defaultBirthDate,
        email: String = "test@example.com",
    ): User {
        given(passwordHasher.encode(rawPassword)).willReturn("encoded_$rawPassword")
        return User.register(
            loginId = loginId,
            rawPassword = rawPassword,
            name = name,
            birthDate = birthDate,
            email = email,
            passwordHasher = passwordHasher,
        )
    }

    @Nested
    @DisplayName("생성")
    inner class Create {
        @Test
        @DisplayName("User 생성 성공 - 비밀번호는 인코딩된 값으로 저장된다")
        fun create_success() {
            // act
            val user = registerUser()

            // assert
            assertAll(
                { assertThat(user.loginId).isEqualTo("testuser1") },
                { assertThat(user.password).isEqualTo("encoded_Password1!") },
                { assertThat(user.name).isEqualTo("홍길동") },
                { assertThat(user.birthDate).isEqualTo(defaultBirthDate) },
                { assertThat(user.email).isEqualTo("test@example.com") },
            )
        }

        @Test
        @DisplayName("생성 실패 - 비밀번호에 생년월일 포함(yyyyMMdd)")
        fun create_passwordContainsBirthDateCompact_throwsException() {
            // act
            val exception = assertThrows<CoreException> {
                User.register(
                    loginId = "testuser1",
                    rawPassword = "Pass19900101!",
                    name = "홍길동",
                    birthDate = defaultBirthDate,
                    email = "test@example.com",
                    passwordHasher = passwordHasher,
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.USER_INVALID_PASSWORD)
        }

        @Test
        @DisplayName("생성 실패 - 비밀번호에 생년월일 포함(yyyy-MM-dd)")
        fun create_passwordContainsBirthDateWithDash_throwsException() {
            // act
            val exception = assertThrows<CoreException> {
                User.register(
                    loginId = "testuser1",
                    rawPassword = "P1990-01-01!",
                    name = "홍길동",
                    birthDate = defaultBirthDate,
                    email = "test@example.com",
                    passwordHasher = passwordHasher,
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.USER_INVALID_PASSWORD)
        }
    }

    @Nested
    @DisplayName("loginId 패턴 검증")
    inner class LoginIdPatternValidation {
        @Test
        @DisplayName("특수문자 포함 - 실패")
        fun create_loginIdWithSpecialChar_throwsException() {
            // act
            val exception = assertThrows<CoreException> {
                User.register(
                    loginId = "test@user",
                    rawPassword = "Password1!",
                    name = "홍길동",
                    birthDate = defaultBirthDate,
                    email = "test@example.com",
                    passwordHasher = passwordHasher,
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.USER_INVALID_LOGIN_ID)
        }

        @Test
        @DisplayName("한글 포함 - 실패")
        fun create_loginIdWithKorean_throwsException() {
            // act
            val exception = assertThrows<CoreException> {
                User.register(
                    loginId = "테스트user",
                    rawPassword = "Password1!",
                    name = "홍길동",
                    birthDate = defaultBirthDate,
                    email = "test@example.com",
                    passwordHasher = passwordHasher,
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.USER_INVALID_LOGIN_ID)
        }
    }

    @Nested
    @DisplayName("password 패턴 검증")
    inner class PasswordPatternValidation {
        @Test
        @DisplayName("허용되지 않은 특수문자(공백) 포함 - 실패")
        fun create_passwordWithInvalidSpecialChar_throwsException() {
            // act
            val exception = assertThrows<CoreException> {
                User.register(
                    loginId = "testuser1",
                    rawPassword = "Pass word1!",
                    name = "홍길동",
                    birthDate = defaultBirthDate,
                    email = "test@example.com",
                    passwordHasher = passwordHasher,
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.USER_INVALID_PASSWORD)
        }
    }

    @Nested
    @DisplayName("이름 마스킹")
    inner class MaskedName {
        @Test
        @DisplayName("2글자 이름 → 마지막 글자가 *로 마스킹된다")
        fun maskedName_twoChars() {
            // arrange
            val user = User.retrieve(
                id = 1L,
                loginId = "testuser1",
                password = "encodedPassword",
                name = "이순",
                birthDate = defaultBirthDate,
                email = "test@example.com",
            )

            // act & assert
            assertThat(user.maskedName).isEqualTo("이*")
        }

        @Test
        @DisplayName("3글자 이름 → 마지막 글자가 *로 마스킹된다")
        fun maskedName_threeChars() {
            // arrange
            val user = User.retrieve(
                id = 1L,
                loginId = "testuser1",
                password = "encodedPassword",
                name = "홍길동",
                birthDate = defaultBirthDate,
                email = "test@example.com",
            )

            // act & assert
            assertThat(user.maskedName).isEqualTo("홍길*")
        }

        @Test
        @DisplayName("15글자 이름 → 마지막 글자가 *로 마스킹된다")
        fun maskedName_fifteenChars() {
            // arrange
            val name = "가나다라마바사아자차카타파하갸"
            val user = User.retrieve(
                id = 1L,
                loginId = "testuser1",
                password = "encodedPassword",
                name = name,
                birthDate = defaultBirthDate,
                email = "test@example.com",
            )

            // act & assert
            assertThat(user.maskedName).isEqualTo("가나다라마바사아자차카타파하*")
            assertThat(user.maskedName).hasSize(15)
        }
    }

    @Nested
    @DisplayName("비밀번호 변경")
    inner class ChangePassword {
        private val currentPassword = "Password1!"
        private val encodedPassword = "encoded_Password1!"

        private fun retrievedUser(
            password: String = encodedPassword,
            birthDate: LocalDate = defaultBirthDate,
        ): User = User.retrieve(
            id = 1L,
            loginId = "testuser1",
            password = password,
            name = "홍길동",
            birthDate = birthDate,
            email = "test@example.com",
        )

        @Test
        @DisplayName("새 비밀번호로 변경하면 인코딩된 비밀번호를 가진 User를 반환한다")
        fun changePassword_success_returnsUpdatedUser() {
            // arrange
            val user = retrievedUser()
            given(passwordHasher.matches(currentPassword, encodedPassword)).willReturn(true)
            given(passwordHasher.matches("NewPassword1!", encodedPassword)).willReturn(false)
            given(passwordHasher.encode("NewPassword1!")).willReturn("encoded_NewPassword1!")

            // act
            val updatedUser = user.changePassword(currentPassword, "NewPassword1!", passwordHasher)

            // assert
            assertAll(
                { assertThat(updatedUser.password).isEqualTo("encoded_NewPassword1!") },
                { assertThat(updatedUser.id).isEqualTo(1L) },
                { assertThat(updatedUser.loginId).isEqualTo("testuser1") },
                { assertThat(updatedUser.name).isEqualTo("홍길동") },
            )
        }

        @Test
        @DisplayName("현재 비밀번호가 일치하지 않으면 실패한다")
        fun changePassword_wrongCurrentPassword_throwsException() {
            // arrange
            val user = retrievedUser()
            given(passwordHasher.matches("WrongPassword1!", encodedPassword)).willReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                user.changePassword("WrongPassword1!", "NewPassword1!", passwordHasher)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.USER_INVALID_PASSWORD)
        }

        @Test
        @DisplayName("새 비밀번호가 현재 비밀번호와 동일하면 실패한다 (해시 기반 비교)")
        fun changePassword_samePassword_throwsException() {
            // arrange
            val user = retrievedUser()
            given(passwordHasher.matches(currentPassword, encodedPassword)).willReturn(true)
            given(passwordHasher.matches(currentPassword, encodedPassword)).willReturn(true)

            // act
            val exception = assertThrows<CoreException> {
                user.changePassword(currentPassword, currentPassword, passwordHasher)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.USER_INVALID_PASSWORD)
        }

        @Test
        @DisplayName("새 비밀번호 패턴이 유효하지 않으면 실패한다")
        fun changePassword_invalidPattern_throwsException() {
            // arrange
            val user = retrievedUser()
            given(passwordHasher.matches(currentPassword, encodedPassword)).willReturn(true)
            given(passwordHasher.matches("Pass word1!", encodedPassword)).willReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                user.changePassword(currentPassword, "Pass word1!", passwordHasher)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.USER_INVALID_PASSWORD)
        }

        @Test
        @DisplayName("새 비밀번호에 생년월일(yyyyMMdd) 포함 시 실패한다")
        fun changePassword_containsBirthDateCompact_throwsException() {
            // arrange
            val user = retrievedUser()
            given(passwordHasher.matches(currentPassword, encodedPassword)).willReturn(true)
            given(passwordHasher.matches("Pass19900101!", encodedPassword)).willReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                user.changePassword(currentPassword, "Pass19900101!", passwordHasher)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.USER_INVALID_PASSWORD)
        }

        @Test
        @DisplayName("새 비밀번호에 생년월일(yyyy-MM-dd) 포함 시 실패한다")
        fun changePassword_containsBirthDateWithDash_throwsException() {
            // arrange
            val user = retrievedUser()
            given(passwordHasher.matches(currentPassword, encodedPassword)).willReturn(true)
            given(passwordHasher.matches("P1990-01-01!", encodedPassword)).willReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                user.changePassword(currentPassword, "P1990-01-01!", passwordHasher)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.USER_INVALID_PASSWORD)
        }
    }

    @Nested
    @DisplayName("name 패턴 검증")
    inner class NamePatternValidation {
        @Test
        @DisplayName("숫자 포함 - 실패")
        fun create_nameWithNumber_throwsException() {
            // act
            val exception = assertThrows<CoreException> {
                User.register(
                    loginId = "testuser1",
                    rawPassword = "Password1!",
                    name = "홍길동1",
                    birthDate = defaultBirthDate,
                    email = "test@example.com",
                    passwordHasher = passwordHasher,
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.USER_INVALID_NAME)
        }

        @Test
        @DisplayName("영문 포함 - 실패")
        fun create_nameWithEnglish_throwsException() {
            // act
            val exception = assertThrows<CoreException> {
                User.register(
                    loginId = "testuser1",
                    rawPassword = "Password1!",
                    name = "Hong길동",
                    birthDate = defaultBirthDate,
                    email = "test@example.com",
                    passwordHasher = passwordHasher,
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.USER_INVALID_NAME)
        }

        @Test
        @DisplayName("공백 포함 - 실패")
        fun create_nameWithSpace_throwsException() {
            // act
            val exception = assertThrows<CoreException> {
                User.register(
                    loginId = "testuser1",
                    rawPassword = "Password1!",
                    name = "홍 길동",
                    birthDate = defaultBirthDate,
                    email = "test@example.com",
                    passwordHasher = passwordHasher,
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.USER_INVALID_NAME)
        }
    }
}
