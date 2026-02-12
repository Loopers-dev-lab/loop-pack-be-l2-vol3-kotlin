package com.loopers.support.util

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThatNoException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

@DisplayName("PasswordValidator")
class PasswordValidatorTest {

    companion object {
        private val BIRTH_DATE = LocalDate.of(1990, 5, 15)
        private const val LOGIN_ID = "test_user1"
        private const val VALID_PASSWORD = "Password1!"
    }

    @DisplayName("validatePassword")
    @Nested
    inner class ValidatePassword {
        @DisplayName("유효한 비밀번호면 예외가 발생하지 않는다")
        @Test
        fun doesNotThrow_whenPasswordIsValid() {
            assertThatNoException().isThrownBy {
                PasswordValidator.validatePassword(VALID_PASSWORD, BIRTH_DATE, LOGIN_ID)
            }
        }

        @DisplayName("loginId 없이도 검증이 가능하다")
        @Test
        fun doesNotThrow_whenLoginIdIsNull() {
            assertThatNoException().isThrownBy {
                PasswordValidator.validatePassword(VALID_PASSWORD, BIRTH_DATE)
            }
        }
    }

    @DisplayName("비밀번호 길이 검증")
    @Nested
    inner class LengthValidation {
        @DisplayName("8자 미만이면 예외가 발생한다")
        @Test
        fun throwsException_whenPasswordIsTooShort() {
            assertThatThrownBy {
                PasswordValidator.validatePassword("Pass1!a", BIRTH_DATE, LOGIN_ID)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
                .hasMessageContaining("8~16자")
        }

        @DisplayName("16자 초과이면 예외가 발생한다")
        @Test
        fun throwsException_whenPasswordIsTooLong() {
            assertThatThrownBy {
                PasswordValidator.validatePassword("Password1!" + "a".repeat(7), BIRTH_DATE, LOGIN_ID)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
                .hasMessageContaining("8~16자")
        }

        @DisplayName("8자이면 허용된다")
        @Test
        fun doesNotThrow_whenPasswordIs8Chars() {
            assertThatNoException().isThrownBy {
                PasswordValidator.validatePassword("Passwo1!", BIRTH_DATE, LOGIN_ID)
            }
        }

        @DisplayName("16자이면 허용된다")
        @Test
        fun doesNotThrow_whenPasswordIs16Chars() {
            assertThatNoException().isThrownBy {
                PasswordValidator.validatePassword("Passwordword12!a", BIRTH_DATE, LOGIN_ID)
            }
        }
    }

    @DisplayName("허용 문자 검증")
    @Nested
    inner class AllowedCharactersValidation {
        @DisplayName("한글이 포함되면 예외가 발생한다")
        @Test
        fun throwsException_whenPasswordContainsKorean() {
            assertThatThrownBy {
                PasswordValidator.validatePassword("Password1!가", BIRTH_DATE, LOGIN_ID)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
                .hasMessageContaining("영문 대소문자, 숫자, 특수문자만")
        }

        @DisplayName("공백이 포함되면 예외가 발생한다")
        @Test
        fun throwsException_whenPasswordContainsSpace() {
            assertThatThrownBy {
                PasswordValidator.validatePassword("Pass word1!", BIRTH_DATE, LOGIN_ID)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
                .hasMessageContaining("영문 대소문자, 숫자, 특수문자만")
        }
    }

    @DisplayName("생년월일 포함 검증")
    @Nested
    inner class BirthDateValidation {
        @DisplayName("YYYYMMDD 형식이 포함되면 예외가 발생한다")
        @Test
        fun throwsException_whenPasswordContainsYYYYMMDD() {
            assertThatThrownBy {
                PasswordValidator.validatePassword("19900515A!", BIRTH_DATE, LOGIN_ID)
            }
                .isInstanceOf(CoreException::class.java)
                .hasMessageContaining("생년월일")
        }

        @DisplayName("YYMMDD 형식이 포함되면 예외가 발생한다")
        @Test
        fun throwsException_whenPasswordContainsYYMMDD() {
            assertThatThrownBy {
                PasswordValidator.validatePassword("A!900515bb", BIRTH_DATE, LOGIN_ID)
            }
                .isInstanceOf(CoreException::class.java)
                .hasMessageContaining("생년월일")
        }

        @DisplayName("MMDD 형식이 포함되면 예외가 발생한다")
        @Test
        fun throwsException_whenPasswordContainsMMDD() {
            assertThatThrownBy {
                PasswordValidator.validatePassword("Abc0515de!", BIRTH_DATE, LOGIN_ID)
            }
                .isInstanceOf(CoreException::class.java)
                .hasMessageContaining("생년월일")
        }
    }

    @DisplayName("최소 2종류 문자 조합 검증")
    @Nested
    inner class MinimumCharacterTypesValidation {
        @DisplayName("영문만 사용하면 예외가 발생한다")
        @Test
        fun throwsException_whenOnlyLetters() {
            assertThatThrownBy {
                PasswordValidator.validatePassword("abcdefgh", BIRTH_DATE, LOGIN_ID)
            }
                .isInstanceOf(CoreException::class.java)
                .hasMessageContaining("2종류 이상")
        }

        @DisplayName("숫자만 사용하면 예외가 발생한다")
        @Test
        fun throwsException_whenOnlyDigits() {
            assertThatThrownBy {
                PasswordValidator.validatePassword("12345678", BIRTH_DATE, LOGIN_ID)
            }
                .isInstanceOf(CoreException::class.java)
                .hasMessageContaining("2종류 이상")
        }

        @DisplayName("특수문자만 사용하면 예외가 발생한다")
        @Test
        fun throwsException_whenOnlySpecialChars() {
            assertThatThrownBy {
                PasswordValidator.validatePassword("!@#\$%^&*", BIRTH_DATE, LOGIN_ID)
            }
                .isInstanceOf(CoreException::class.java)
                .hasMessageContaining("2종류 이상")
        }

        @DisplayName("영문+숫자 조합은 허용된다")
        @Test
        fun doesNotThrow_whenLettersAndDigits() {
            assertThatNoException().isThrownBy {
                PasswordValidator.validatePassword("Aqwert1x", BIRTH_DATE, LOGIN_ID)
            }
        }

        @DisplayName("영문+특수문자 조합은 허용된다")
        @Test
        fun doesNotThrow_whenLettersAndSpecialChars() {
            assertThatNoException().isThrownBy {
                PasswordValidator.validatePassword("Aqwert!x", BIRTH_DATE, LOGIN_ID)
            }
        }

        @DisplayName("숫자+특수문자 조합은 허용된다")
        @Test
        fun doesNotThrow_whenDigitsAndSpecialChars() {
            assertThatNoException().isThrownBy {
                PasswordValidator.validatePassword("19284756!", BIRTH_DATE, LOGIN_ID)
            }
        }
    }

    @DisplayName("연속 동일문자 검증")
    @Nested
    inner class ConsecutiveRepeatingCharsValidation {
        @DisplayName("동일문자가 3개 연속이면 예외가 발생한다")
        @ParameterizedTest
        @ValueSource(strings = ["Paaassw1!", "Pass111w!", "Pass!!!wd"])
        fun throwsException_whenThreeConsecutiveRepeatingChars(password: String) {
            assertThatThrownBy {
                PasswordValidator.validatePassword(password, BIRTH_DATE, LOGIN_ID)
            }
                .isInstanceOf(CoreException::class.java)
                .hasMessageContaining("연속 동일문자")
        }

        @DisplayName("동일문자가 2개까지는 허용된다")
        @Test
        fun doesNotThrow_whenTwoConsecutiveRepeatingChars() {
            assertThatNoException().isThrownBy {
                PasswordValidator.validatePassword("Paassw1!", BIRTH_DATE, LOGIN_ID)
            }
        }
    }

    @DisplayName("연속 순서문자 검증")
    @Nested
    inner class ConsecutiveSequentialCharsValidation {
        @DisplayName("연속 오름차순 문자 3개이면 예외가 발생한다 (abc)")
        @ParameterizedTest
        @ValueSource(strings = ["Pabcssw1!", "P123ssw!a", "Pdefssw1!"])
        fun throwsException_whenThreeConsecutiveAscendingChars(password: String) {
            assertThatThrownBy {
                PasswordValidator.validatePassword(password, BIRTH_DATE, LOGIN_ID)
            }
                .isInstanceOf(CoreException::class.java)
                .hasMessageContaining("연속 순서문자")
        }

        @DisplayName("연속 내림차순 문자 3개이면 예외가 발생한다 (cba)")
        @ParameterizedTest
        @ValueSource(strings = ["Pcbassw1!", "P321ssw!a", "Pfedssw1!"])
        fun throwsException_whenThreeConsecutiveDescendingChars(password: String) {
            assertThatThrownBy {
                PasswordValidator.validatePassword(password, BIRTH_DATE, LOGIN_ID)
            }
                .isInstanceOf(CoreException::class.java)
                .hasMessageContaining("연속 순서문자")
        }

        @DisplayName("연속되지 않은 순서문자는 허용된다")
        @Test
        fun doesNotThrow_whenNonConsecutiveSequentialChars() {
            assertThatNoException().isThrownBy {
                PasswordValidator.validatePassword("Pa1ceg2!", BIRTH_DATE, LOGIN_ID)
            }
        }
    }

    @DisplayName("로그인 ID 포함 검증")
    @Nested
    inner class LoginIdValidation {
        @DisplayName("비밀번호에 로그인 ID가 포함되면 예외가 발생한다")
        @Test
        fun throwsException_whenPasswordContainsLoginId() {
            assertThatThrownBy {
                PasswordValidator.validatePassword("test_user1!", BIRTH_DATE, "test_user1")
            }
                .isInstanceOf(CoreException::class.java)
                .hasMessageContaining("로그인 ID")
        }

        @DisplayName("로그인 ID가 null이면 검증을 건너뛴다")
        @Test
        fun doesNotThrow_whenLoginIdIsNull() {
            assertThatNoException().isThrownBy {
                PasswordValidator.validatePassword(VALID_PASSWORD, BIRTH_DATE, null)
            }
        }
    }
}
