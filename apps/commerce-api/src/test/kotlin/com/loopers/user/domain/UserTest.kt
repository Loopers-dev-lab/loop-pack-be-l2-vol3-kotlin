package com.loopers.user.domain

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

@DisplayName("User 도메인")
class UserTest {
    private val defaultBirthDate = LocalDate.of(1990, 1, 1)

    @Nested
    @DisplayName("생성")
    inner class Create {
        @Test
        @DisplayName("User 생성 성공")
        fun create_success() {
            // act
            val user = User.register(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = defaultBirthDate,
                email = "test@example.com",
            )

            // assert
            assertAll(
                { assertThat(user.loginId).isEqualTo("testuser1") },
                { assertThat(user.password).isEqualTo("Password1!") },
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
                    password = "Pass19900101!",
                    name = "홍길동",
                    birthDate = defaultBirthDate,
                    email = "test@example.com",
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
                    password = "P1990-01-01!",
                    name = "홍길동",
                    birthDate = defaultBirthDate,
                    email = "test@example.com",
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
                    password = "Password1!",
                    name = "홍길동",
                    birthDate = defaultBirthDate,
                    email = "test@example.com",
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
                    password = "Password1!",
                    name = "홍길동",
                    birthDate = defaultBirthDate,
                    email = "test@example.com",
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
                    password = "Pass word1!",
                    name = "홍길동",
                    birthDate = defaultBirthDate,
                    email = "test@example.com",
                )
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
                    password = "Password1!",
                    name = "홍길동1",
                    birthDate = defaultBirthDate,
                    email = "test@example.com",
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
                    password = "Password1!",
                    name = "Hong길동",
                    birthDate = defaultBirthDate,
                    email = "test@example.com",
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
                    password = "Password1!",
                    name = "홍 길동",
                    birthDate = defaultBirthDate,
                    email = "test@example.com",
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.USER_INVALID_NAME)
        }
    }
}
