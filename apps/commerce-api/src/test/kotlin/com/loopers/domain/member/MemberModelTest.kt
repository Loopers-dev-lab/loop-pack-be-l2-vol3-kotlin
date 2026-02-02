package com.loopers.domain.member

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class MemberModelTest {
    @DisplayName("회원가입 할 때")
    @Nested
    inner class Create {
        @DisplayName("모든 필드가 유효하면, 정상적으로 생성된다.")
        @Test
        fun createsMemberModel_whenAllFieldsAreValid() {
            // arrange
            val loginId = "testuser123"
            val password = "Test1234!@#"
            val name = "홍길동"
            val email = "test@example.com"
            val birthDate = LocalDate.of(1990, 1, 1)

            // act
            val member = MemberModel(
                loginId = loginId,
                password = password,
                name = name,
                email = email,
                birthDate = birthDate,
            )

            // assert
            assertAll(
                { Assertions.assertThat(member.id).isEqualTo(0L) },
                { Assertions.assertThat(member.loginId).isEqualTo(loginId) },
                { Assertions.assertThat(member.password).isEqualTo(password) },
                { Assertions.assertThat(member.name).isEqualTo(name) },
                { Assertions.assertThat(member.email).isEqualTo(email) },
                { Assertions.assertThat(member.birthDate).isEqualTo(birthDate) },
            )
        }

        @DisplayName("loginId가 영문/숫자가 아니면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenLoginIdContainsSpecialCharacters() {
            // arrange
            val loginId = "test@user"

            // act
            val result = assertThrows<CoreException> {
                MemberModel(
                    loginId = loginId,
                    password = "Test1234!@#",
                    name = "홍길동",
                    email = "test@example.com",
                    birthDate = LocalDate.of(1990, 1, 1),
                )
            }

            // assert
            Assertions.assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("loginId가 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenLoginIdIsBlank() {
            // arrange
            val loginId = "   "

            // act
            val result = assertThrows<CoreException> {
                MemberModel(
                    loginId = loginId,
                    password = "Test1234!@#",
                    name = "홍길동",
                    email = "test@example.com",
                    birthDate = LocalDate.of(1990, 1, 1),
                )
            }

            // assert
            Assertions.assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호가 8자 미만이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenPasswordLengthIsLessThan8() {
            // arrange
            val password = "abc123!"

            // act
            val result = assertThrows<CoreException> {
                MemberModel(
                    loginId = "testuser123",
                    password = password,
                    name = "홍길동",
                    email = "test@example.com",
                    birthDate = LocalDate.of(1990, 1, 1),
                )
            }

            // assert
            Assertions.assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호가 16자를 초과하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenPasswordLengthIsGreaterThan16() {
            // arrange
            val password = "abcd1234!abcd1234!" // 17자 이상

            // act
            val result = assertThrows<CoreException> {
                MemberModel(
                    loginId = "testuser123",
                    password = password,
                    name = "홍길동",
                    email = "test@example.com",
                    birthDate = LocalDate.of(1990, 1, 1),
                )
            }

            // assert
            Assertions.assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호에 허용되지 않은 문자가 있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenPasswordContainsInvalidChar() {
            // arrange
            val password = "Test1234!한글"

            // act
            val result = assertThrows<CoreException> {
                MemberModel(
                    loginId = "testuser123",
                    password = password,
                    name = "홍길동",
                    email = "test@example.com",
                    birthDate = LocalDate.of(1990, 1, 1),
                )
            }

            // assert
            Assertions.assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호가 8~16자의 영문 대소문자, 숫자, 특수문자로만 구성되면 회원 생성에 성공한다.")
        @Test
        fun createsMemberSuccessfully_whenPasswordIsValid() {
            // arrange
            val password = "Test1234!@#"

            // act & assert
            assertDoesNotThrow {
                MemberModel(
                    loginId = "testuser123",
                    password = password,
                    name = "홍길동",
                    email = "test@example.com",
                    birthDate = LocalDate.of(1990, 1, 1),
                )
            }
        }

        @DisplayName("비밀번호에 YYYYMMDD 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenPasswordContainsBirthDate() {
            // arrange
            val password = "Test19900101!@#"
            val birthDate = LocalDate.of(1990, 1, 1)

            // act
            val result = assertThrows<CoreException> {
                MemberModel(
                    loginId = "testuser123",
                    password = password,
                    name = "홍길동",
                    email = "test@example.com",
                    birthDate = birthDate,
                )
            }

            // assert
            Assertions.assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호에 생년월일(YYMMDD)이 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenPasswordContainsBirthDateAsYYMMDD() {
            // arrange
            val password = "Test900101!@#"
            val birthDate = LocalDate.of(1990, 1, 1)

            // act
            val result = assertThrows<CoreException> {
                MemberModel(
                    loginId = "testuser123",
                    password = password,
                    name = "홍길동",
                    email = "test@example.com",
                    birthDate = birthDate,
                )
            }

            // assert
            Assertions.assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호에 생년월일(MMDD)이 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenPasswordContainsBirthDateAsMMDD() {
            // arrange
            val password = "Test0101!@#"
            val birthDate = LocalDate.of(1990, 1, 1)

            // act
            val result = assertThrows<CoreException> {
                MemberModel(
                    loginId = "testuser123",
                    password = password,
                    name = "홍길동",
                    email = "test@example.com",
                    birthDate = birthDate,
                )
            }

            // assert
            Assertions.assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("이름이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenNameIsBlank() {
            // arrange
            val name = "   "

            // act
            val result = assertThrows<CoreException> {
                MemberModel(
                    loginId = "testuser123",
                    password = "Test1234!@#",
                    name = name,
                    email = "test@example.com",
                    birthDate = LocalDate.of(1990, 1, 1),
                )
            }

            // assert
            Assertions.assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("email 형식이 잘못되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenEmailFormatIsInvalid() {
            // arrange
            val email = "invalid-email"

            // act
            val result = assertThrows<CoreException> {
                MemberModel(
                    loginId = "testuser123",
                    password = "Test1234!@#",
                    name = "홍길동",
                    email = email,
                    birthDate = LocalDate.of(1990, 1, 1),
                )
            }

            // assert
            Assertions.assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("생년월일이 미래일이면, BAD_REQUEST 예외가 발생한다.")
    @Test
    fun throwsBadRequestException_whenBirthDateIsInFuture() {
        // arrange
        val birthDate = LocalDate.now().plusDays(1)

        // act
        val result = assertThrows<CoreException> {
            MemberModel(
                loginId = "testuser123",
                password = "Test1234!@#",
                name = "홍길동",
                email = "test@example.com",
                birthDate = birthDate,
            )
        }

        // assert
        Assertions.assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
    }

    @DisplayName("생년월일이 오늘이면, 회원 생성에 성공한다.")
    @Test
    fun createsMemberSuccessfully_whenBirthDateIsToday() {
        // arrange
        val birthDate = LocalDate.now()

        // act & assert
        assertDoesNotThrow {
            MemberModel(
                loginId = "testuser123",
                password = "Test1234!@#",
                name = "홍길동",
                email = "test@example.com",
                birthDate = birthDate,
            )
        }
    }
}
