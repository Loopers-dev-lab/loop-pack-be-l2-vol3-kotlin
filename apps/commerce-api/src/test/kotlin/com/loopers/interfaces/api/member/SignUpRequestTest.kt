package com.loopers.interfaces.api.member

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("SignUpRequest 비밀번호 검증")
class SignUpRequestTest {

    companion object {
        private const val VALID_LOGIN_ID = "test_user1"
        private const val VALID_PASSWORD = "Password1!"
        private const val VALID_NAME = "홍길동"
        private const val VALID_EMAIL = "test@example.com"
        private val VALID_BIRTH_DATE = LocalDate.of(1990, 5, 15)
    }

    @DisplayName("정상 생성")
    @Nested
    inner class Create {
        @DisplayName("모든 필드가 유효하면 SignUpRequest가 생성된다")
        @Test
        fun createsSignUpRequest_whenAllFieldsAreValid() {
            // arrange & act
            val request = MemberV1Dto.SignUpRequest(
                loginId = VALID_LOGIN_ID,
                password = VALID_PASSWORD,
                name = VALID_NAME,
                birthDate = VALID_BIRTH_DATE,
                email = VALID_EMAIL,
            )

            // assert
            assertThat(request.loginId).isEqualTo(VALID_LOGIN_ID)
            assertThat(request.password).isEqualTo(VALID_PASSWORD)
        }
    }

    @DisplayName("비밀번호 검증")
    @Nested
    inner class PasswordValidation {
        @DisplayName("비밀번호가 8자 미만이면 예외가 발생한다")
        @Test
        fun throwsException_whenPasswordIsTooShort() {
            // arrange & act & assert
            assertThatThrownBy {
                MemberV1Dto.SignUpRequest(
                    loginId = VALID_LOGIN_ID,
                    password = "Pass1!",
                    name = VALID_NAME,
                    birthDate = VALID_BIRTH_DATE,
                    email = VALID_EMAIL,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호가 16자 초과이면 예외가 발생한다")
        @Test
        fun throwsException_whenPasswordIsTooLong() {
            // arrange & act & assert
            assertThatThrownBy {
                MemberV1Dto.SignUpRequest(
                    loginId = VALID_LOGIN_ID,
                    password = "Password1!" + "a".repeat(7),
                    name = VALID_NAME,
                    birthDate = VALID_BIRTH_DATE,
                    email = VALID_EMAIL,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호에 허용되지 않은 문자가 포함되면 예외가 발생한다")
        @Test
        fun throwsException_whenPasswordContainsInvalidCharacters() {
            // arrange & act & assert
            assertThatThrownBy {
                MemberV1Dto.SignUpRequest(
                    loginId = VALID_LOGIN_ID,
                    password = "Password1!한글",
                    name = VALID_NAME,
                    birthDate = VALID_BIRTH_DATE,
                    email = VALID_EMAIL,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호에 생년월일(YYYYMMDD)이 포함되면 예외가 발생한다")
        @Test
        fun throwsException_whenPasswordContainsBirthDateYYYYMMDD() {
            // arrange & act & assert
            assertThatThrownBy {
                MemberV1Dto.SignUpRequest(
                    loginId = VALID_LOGIN_ID,
                    password = "19900515A!",
                    name = VALID_NAME,
                    birthDate = VALID_BIRTH_DATE,
                    email = VALID_EMAIL,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호에 생년월일(YYMMDD)이 포함되면 예외가 발생한다")
        @Test
        fun throwsException_whenPasswordContainsBirthDateYYMMDD() {
            // arrange & act & assert
            assertThatThrownBy {
                MemberV1Dto.SignUpRequest(
                    loginId = VALID_LOGIN_ID,
                    password = "Aa900515!!",
                    name = VALID_NAME,
                    birthDate = VALID_BIRTH_DATE,
                    email = VALID_EMAIL,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호에 생년월일(MMDD)이 포함되면 예외가 발생한다")
        @Test
        fun throwsException_whenPasswordContainsBirthDateMMDD() {
            // arrange & act & assert
            assertThatThrownBy {
                MemberV1Dto.SignUpRequest(
                    loginId = VALID_LOGIN_ID,
                    password = "Aa0515abc!",
                    name = VALID_NAME,
                    birthDate = VALID_BIRTH_DATE,
                    email = VALID_EMAIL,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }
    }
}
