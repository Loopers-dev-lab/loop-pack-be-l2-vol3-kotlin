package com.loopers.domain.member

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

@DisplayName("회원 모델 테스트")
class MemberModelTest {

    @DisplayName("회원을 생성할 때, ")
    @Nested
    inner class Create {

        @DisplayName("모든 정보가 유효하면, 정상적으로 생성된다.")
        @Test
        fun createsMember_whenAllFieldsAreValid() {
            // arrange
            val loginId = "testuser123"
            val password = "Valid@Pass123"
            val name = "홍길동"
            val birthDate = LocalDate.of(1990, 1, 1)
            val email = "test@example.com"

            // act
            val member = MemberModel(
                loginId = loginId,
                password = password,
                name = name,
                birthDate = birthDate,
                email = email,
            )

            // assert
            assertAll(
                { assertThat(member.loginId).isEqualTo(loginId) },
                { assertThat(member.password).isEqualTo(password) },
                { assertThat(member.name).isEqualTo(name) },
                { assertThat(member.birthDate).isEqualTo(birthDate) },
                { assertThat(member.email).isEqualTo(email) },
            )
        }
    }

    @DisplayName("로그인ID를 검증할 때, ")
    @Nested
    inner class ValidateLoginId {

        @DisplayName("빈값이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenLoginIdIsBlank() {
            // arrange
            val loginId = ""

            // act & assert
            val exception = assertThrows<CoreException> {
                MemberModel(
                    loginId = loginId,
                    password = "Valid@Pass123",
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 1),
                    email = "test@example.com",
                )
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("영문과 숫자가 아닌 한글이 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenLoginIdContainsKorean() {
            // arrange
            val loginId = "테스트123"

            // act & assert
            val exception = assertThrows<CoreException> {
                MemberModel(
                    loginId = loginId,
                    password = "Valid@Pass123",
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 1),
                    email = "test@example.com",
                )
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("영문과 숫자가 아닌 특수문자가 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenLoginIdContainsSpecialCharacters() {
            // arrange
            val loginId = "test@user123"

            // act & assert
            val exception = assertThrows<CoreException> {
                MemberModel(
                    loginId = loginId,
                    password = "Valid@Pass123",
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 1),
                    email = "test@example.com",
                )
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("비밀번호를 검증할 때, ")
    @Nested
    inner class ValidatePassword {

        @DisplayName("8자 미만이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPasswordIsLessThan8Characters() {
            // arrange
            val password = "Pass@12"

            // act & assert
            val exception = assertThrows<CoreException> {
                MemberModel(
                    loginId = "testuser123",
                    password = password,
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 1),
                    email = "test@example.com",
                )
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("16자 초과이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPasswordIsMoreThan16Characters() {
            // arrange
            val password = "ValidPassword@1234"

            // act & assert
            val exception = assertThrows<CoreException> {
                MemberModel(
                    loginId = "testuser123",
                    password = password,
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 1),
                    email = "test@example.com",
                )
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("영문, 숫자, 특수문자 외 문자가 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPasswordContainsInvalidCharacters() {
            // arrange
            val password = "Valid@한글123"

            // act & assert
            val exception = assertThrows<CoreException> {
                MemberModel(
                    loginId = "testuser123",
                    password = password,
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 1),
                    email = "test@example.com",
                )
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("생년월일(yyyyMMdd 형식)이 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPasswordContainsBirthDateInYyyyMMddFormat() {
            // arrange
            val birthDate = LocalDate.of(1990, 1, 1)
            val password = "Pass19900101@"

            // act & assert
            val exception = assertThrows<CoreException> {
                MemberModel(
                    loginId = "testuser123",
                    password = password,
                    name = "홍길동",
                    birthDate = birthDate,
                    email = "test@example.com",
                )
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("생년월일(yyyy-MM-dd 형식)이 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPasswordContainsBirthDateInYyyyDashMMDashDdFormat() {
            // arrange
            val birthDate = LocalDate.of(1990, 1, 1)
            val password = "Pass1990-01-01"

            // act & assert
            val exception = assertThrows<CoreException> {
                MemberModel(
                    loginId = "testuser123",
                    password = password,
                    name = "홍길동",
                    birthDate = birthDate,
                    email = "test@example.com",
                )
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("이름을 검증할 때, ")
    @Nested
    inner class ValidateName {

        @DisplayName("빈값이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNameIsBlank() {
            // arrange
            val name = ""

            // act & assert
            val exception = assertThrows<CoreException> {
                MemberModel(
                    loginId = "testuser123",
                    password = "Valid@Pass123",
                    name = name,
                    birthDate = LocalDate.of(1990, 1, 1),
                    email = "test@example.com",
                )
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("이메일을 검증할 때, ")
    @Nested
    inner class ValidateEmail {

        @DisplayName("빈값이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenEmailIsBlank() {
            // arrange
            val email = ""

            // act & assert
            val exception = assertThrows<CoreException> {
                MemberModel(
                    loginId = "testuser123",
                    password = "Valid@Pass123",
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 1),
                    email = email,
                )
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("이메일 형식이 아니면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenEmailIsInvalidFormat() {
            // arrange
            val email = "invalid-email"

            // act & assert
            val exception = assertThrows<CoreException> {
                MemberModel(
                    loginId = "testuser123",
                    password = "Valid@Pass123",
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 1),
                    email = email,
                )
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("비밀번호를 변경할 때, ")
    @Nested
    inner class ChangePassword {

        @DisplayName("유효한 새 비밀번호면, 정상적으로 변경된다.")
        @Test
        fun changesPassword_whenNewPasswordIsValid() {
            // arrange
            val member = MemberModel(
                loginId = "testuser123",
                password = "OldPass@123",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "test@example.com",
            )
            val newPassword = "NewPass@456"

            // act
            member.changePassword(newPassword)

            // assert
            assertThat(member.password).isEqualTo(newPassword)
        }

        @DisplayName("현재 비밀번호와 동일하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNewPasswordIsSameAsCurrent() {
            // arrange
            val currentPassword = "CurrentPass@123"
            val member = MemberModel(
                loginId = "testuser123",
                password = currentPassword,
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "test@example.com",
            )

            // act & assert
            val exception = assertThrows<CoreException> {
                member.changePassword(currentPassword)
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("새 비밀번호가 규칙을 위반하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNewPasswordViolatesRules() {
            // arrange
            val member = MemberModel(
                loginId = "testuser123",
                password = "OldPass@123",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "test@example.com",
            )
            val newPassword = "short"

            // act & assert
            val exception = assertThrows<CoreException> {
                member.changePassword(newPassword)
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
