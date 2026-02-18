package com.loopers.domain.user.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EmailTest {
    @DisplayName("이메일을 생성할 때, ")
    @Nested
    inner class Create {
        @DisplayName("유효한 이메일로 생성하면 Email 객체를 반환한다")
        @Test
        fun createsEmail_whenValidEmailIsProvided() {
            // arrange
            val validEmail = "test@example.com"

            // act
            val email = Email.of(validEmail)

            // assert
            assertThat(email.value).isEqualTo(validEmail)
        }

        @DisplayName("유효한 이메일(user.name@company.co.kr)로 생성하면 Email 객체를 반환한다")
        @Test
        fun createsEmail_whenValidEmailWithDotIsProvided() {
            // arrange
            val validEmail = "user.name@company.co.kr"

            // act
            val email = Email.of(validEmail)

            // assert
            assertThat(email.value).isEqualTo(validEmail)
        }

        @DisplayName("이메일이 비어있으면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenEmailIsBlank() {
            // arrange
            val blankEmail = ""

            // act
            val result = assertThrows<CoreException> {
                Email.of(blankEmail)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("이메일에 @가 없으면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenEmailDoesNotContainAtSign() {
            // arrange
            val invalidEmail = "invalid-email"

            // act
            val result = assertThrows<CoreException> {
                Email.of(invalidEmail)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("이메일의 도메인이 없으면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenEmailHasNoDomain() {
            // arrange
            val invalidEmail = "test@.com"

            // act
            val result = assertThrows<CoreException> {
                Email.of(invalidEmail)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("이메일에 최상위 도메인(TLD)이 없으면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenEmailHasNoTopLevelDomain() {
            // arrange
            val invalidEmail = "test@domain"

            // act
            val result = assertThrows<CoreException> {
                Email.of(invalidEmail)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("이메일의 로컬 파트(local part)가 없으면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenEmailHasNoLocalPart() {
            // arrange
            val invalidEmail = "@example.com"

            // act
            val result = assertThrows<CoreException> {
                Email.of(invalidEmail)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("유효한 이메일(특수문자 포함)로 생성하면 Email 객체를 반환한다")
        @Test
        fun createsEmail_whenEmailContainsSpecialCharacters() {
            // arrange
            val validEmail = "user+tag@example.com"

            // act
            val email = Email.of(validEmail)

            // assert
            assertThat(email.value).isEqualTo(validEmail)
        }

        @DisplayName("유효한 이메일(하이픈 포함)로 생성하면 Email 객체를 반환한다")
        @Test
        fun createsEmail_whenEmailContainsHyphen() {
            // arrange
            val validEmail = "user-name@example.com"

            // act
            val email = Email.of(validEmail)

            // assert
            assertThat(email.value).isEqualTo(validEmail)
        }

        @DisplayName("유효한 이메일(언더스코어 포함)로 생성하면 Email 객체를 반환한다")
        @Test
        fun createsEmail_whenEmailContainsUnderscore() {
            // arrange
            val validEmail = "user_name@example.com"

            // act
            val email = Email.of(validEmail)

            // assert
            assertThat(email.value).isEqualTo(validEmail)
        }
    }
}
