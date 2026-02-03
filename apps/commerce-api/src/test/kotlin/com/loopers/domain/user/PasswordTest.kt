package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.UserErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class PasswordTest {

    private val birthDate = LocalDate.of(2000, 9, 30)

    @DisplayName("비밀번호 생성")
    @Nested
    inner class Create {

        @DisplayName("8자 이상 16자 이하의 영문, 숫자, 특수문자 조합이면 성공한다.")
        @Test
        fun success() {
            // arrange
            val rawPassword = "Test123!"

            // act
            val password = Password.of(rawPassword, birthDate)

            // assert
            assertThat(password.value).isEqualTo(rawPassword)
        }
    }

    @DisplayName("비밀번호 생성 실패: 길이")
    @Nested
    inner class FailByLength {

        @DisplayName("8자 미만이면 실패한다.")
        @Test
        fun failWhenLessThan8Characters() {
            // arrange
            val rawPassword = "Test12!"  // 7자

            // act & assert
            val exception = assertThrows<CoreException> {
                Password.of(rawPassword, birthDate)
            }
            assertThat(exception.errorCode).isEqualTo(UserErrorCode.INVALID_PASSWORD_LENGTH)
        }

        @DisplayName("16자 초과이면 실패한다.")
        @Test
        fun failWhenMoreThan16Characters() {
            // arrange
            val rawPassword = "Test1234567890!@#"  // 17자

            // act & assert
            val exception = assertThrows<CoreException> {
                Password.of(rawPassword, birthDate)
            }
            assertThat(exception.errorCode).isEqualTo(UserErrorCode.INVALID_PASSWORD_LENGTH)
        }
    }

    @DisplayName("비밀번호 생성 실패: 허용되지 않는 문자")
    @Nested
    inner class FailByInvalidCharacter {

        @DisplayName("한국어를 포함하면 실패한다.")
        @Test
        fun failWhenContainsKorean() {
            // arrange
            val rawPassword = "안예원123!!"

            // act & assert
            val exception = assertThrows<CoreException> {
                Password.of(rawPassword, birthDate)
            }
            assertThat(exception.errorCode).isEqualTo(UserErrorCode.INVALID_PASSWORD_FORMAT)
        }
    }

    @DisplayName("비밀번호 생성 실패: 생년월일 포함")
    @Nested
    inner class FailByBirthDate {

        @DisplayName("생년월일(YYYYMMDD)이 포함되면 실패한다.")
        @Test
        fun failWhenContainsBirthDate() {
            // arrange
            val rawPassword = "Pass20000930!"

            // act & assert
            val exception = assertThrows<CoreException> {
                Password.of(rawPassword, birthDate)
            }
            assertThat(exception.errorCode).isEqualTo(UserErrorCode.PASSWORD_CONTAINS_BIRTH_DATE)
        }
    }
}
