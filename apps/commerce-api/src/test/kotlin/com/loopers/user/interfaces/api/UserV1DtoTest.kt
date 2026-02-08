package com.loopers.user.interfaces.api

import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("UserV1Dto 유효성 검증")
class UserV1DtoTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    private fun createRequest(
        loginId: String = "testuser1",
        password: String = "Password1!",
        name: String = "홍길동",
        birthDate: LocalDate = LocalDate.of(1990, 1, 1),
        email: String = "test@example.com",
    ): UserV1Dto.SignUpRequest = UserV1Dto.SignUpRequest(loginId, password, name, birthDate, email)

    @Nested
    @DisplayName("loginId 검증")
    inner class LoginIdValidation {
        @Test
        @DisplayName("4자(최소) → 통과")
        fun validate_loginId4chars_success() {
            val request = createRequest(loginId = "test")
            val violations = validator.validate(request)
            assertThat(violations).isEmpty()
        }

        @Test
        @DisplayName("3자 → 실패")
        fun validate_loginId3chars_fail() {
            val request = createRequest(loginId = "tes")
            val violations = validator.validate(request)
            assertThat(violations).isNotEmpty()
        }

        @Test
        @DisplayName("20자(최대) → 통과")
        fun validate_loginId20chars_success() {
            val request = createRequest(loginId = "a".repeat(20))
            val violations = validator.validate(request)
            assertThat(violations).isEmpty()
        }

        @Test
        @DisplayName("21자 → 실패")
        fun validate_loginId21chars_fail() {
            val request = createRequest(loginId = "a".repeat(21))
            val violations = validator.validate(request)
            assertThat(violations).isNotEmpty()
        }

        @Test
        @DisplayName("빈 문자열 → 실패")
        fun validate_loginIdBlank_fail() {
            val request = createRequest(loginId = "")
            val violations = validator.validate(request)
            assertThat(violations).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("password 검증")
    inner class PasswordValidation {
        @Test
        @DisplayName("8자(최소) → 통과")
        fun validate_password8chars_success() {
            val request = createRequest(password = "Passwo1!")
            val violations = validator.validate(request)
            assertThat(violations).isEmpty()
        }

        @Test
        @DisplayName("7자 → 실패")
        fun validate_password7chars_fail() {
            val request = createRequest(password = "Passw1!")
            val violations = validator.validate(request)
            assertThat(violations).isNotEmpty()
        }

        @Test
        @DisplayName("16자(최대) → 통과")
        fun validate_password16chars_success() {
            val request = createRequest(password = "Password1!aaaaaa")
            val violations = validator.validate(request)
            assertThat(violations).isEmpty()
        }

        @Test
        @DisplayName("17자 → 실패")
        fun validate_password17chars_fail() {
            val request = createRequest(password = "Password1!aaaaaaa")
            val violations = validator.validate(request)
            assertThat(violations).isNotEmpty()
        }

        @Test
        @DisplayName("빈 문자열 → 실패")
        fun validate_passwordBlank_fail() {
            val request = createRequest(password = "")
            val violations = validator.validate(request)
            assertThat(violations).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("name 검증")
    inner class NameValidation {
        @Test
        @DisplayName("2자(최소) → 통과")
        fun validate_name2chars_success() {
            val request = createRequest(name = "홍길")
            val violations = validator.validate(request)
            assertThat(violations).isEmpty()
        }

        @Test
        @DisplayName("1자 → 실패")
        fun validate_name1char_fail() {
            val request = createRequest(name = "홍")
            val violations = validator.validate(request)
            assertThat(violations).isNotEmpty()
        }

        @Test
        @DisplayName("15자(최대) → 통과")
        fun validate_name15chars_success() {
            val request = createRequest(name = "가".repeat(15))
            val violations = validator.validate(request)
            assertThat(violations).isEmpty()
        }

        @Test
        @DisplayName("16자 → 실패")
        fun validate_name16chars_fail() {
            val request = createRequest(name = "가".repeat(16))
            val violations = validator.validate(request)
            assertThat(violations).isNotEmpty()
        }

        @Test
        @DisplayName("빈 문자열 → 실패")
        fun validate_nameBlank_fail() {
            val request = createRequest(name = "")
            val violations = validator.validate(request)
            assertThat(violations).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("birthDate 검증")
    inner class BirthDateValidation {
        @Test
        @DisplayName("과거 날짜 → 통과")
        fun validate_pastDate_success() {
            val request = createRequest(birthDate = LocalDate.of(1990, 1, 1))
            val violations = validator.validate(request)
            assertThat(violations).isEmpty()
        }

        @Test
        @DisplayName("오늘 날짜 → 실패")
        fun validate_today_fail() {
            val request = createRequest(birthDate = LocalDate.now())
            val violations = validator.validate(request)
            assertThat(violations).isNotEmpty()
        }

        @Test
        @DisplayName("미래 날짜 → 실패")
        fun validate_futureDate_fail() {
            val request = createRequest(birthDate = LocalDate.now().plusDays(1))
            val violations = validator.validate(request)
            assertThat(violations).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("email 검증")
    inner class EmailValidation {
        @Test
        @DisplayName("유효한 이메일 → 통과")
        fun validate_validEmail_success() {
            val request = createRequest(email = "test@example.com")
            val violations = validator.validate(request)
            assertThat(violations).isEmpty()
        }

        @Test
        @DisplayName("@ 없음 → 실패")
        fun validate_emailWithoutAt_fail() {
            val request = createRequest(email = "testexample.com")
            val violations = validator.validate(request)
            assertThat(violations).isNotEmpty()
        }

        @Test
        @DisplayName("빈 문자열 → 실패")
        fun validate_emailBlank_fail() {
            val request = createRequest(email = "")
            val violations = validator.validate(request)
            assertThat(violations).isNotEmpty()
        }
    }
}
