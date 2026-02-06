package com.loopers.domain.member

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("MemberModel")
class MemberModelTest {

    companion object {
        private const val VALID_LOGIN_ID = "test_user1"
        private const val VALID_ENCODED_PASSWORD = "\$2a\$10\$encodedPasswordHash"
        private const val VALID_NAME = "홍길동"
        private const val VALID_EMAIL = "test@example.com"
        private val VALID_BIRTH_DATE = LocalDate.of(1990, 5, 15)
    }

    @DisplayName("정상 생성")
    @Nested
    inner class Create {
        @DisplayName("모든 필드가 유효하면 MemberModel이 생성된다")
        @Test
        fun createsMemberModel_whenAllFieldsAreValid() {
            // arrange & act
            val member = MemberModel(
                loginId = VALID_LOGIN_ID,
                password = VALID_ENCODED_PASSWORD,
                name = VALID_NAME,
                birthDate = VALID_BIRTH_DATE,
                email = VALID_EMAIL,
            )

            // assert
            assertThat(member.loginId).isEqualTo(VALID_LOGIN_ID)
            assertThat(member.password).isEqualTo(VALID_ENCODED_PASSWORD)
            assertThat(member.name).isEqualTo(VALID_NAME)
            assertThat(member.birthDate).isEqualTo(VALID_BIRTH_DATE)
            assertThat(member.email).isEqualTo(VALID_EMAIL)
        }
    }

    @DisplayName("loginId 검증")
    @Nested
    inner class LoginIdValidation {
        @DisplayName("loginId가 빈 값이면 예외가 발생한다")
        @Test
        fun throwsException_whenLoginIdIsEmpty() {
            // arrange & act & assert
            assertThatThrownBy {
                MemberModel(
                    loginId = "",
                    password = VALID_ENCODED_PASSWORD,
                    name = VALID_NAME,
                    birthDate = VALID_BIRTH_DATE,
                    email = VALID_EMAIL,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("loginId가 4자 미만이면 예외가 발생한다")
        @Test
        fun throwsException_whenLoginIdIsTooShort() {
            // arrange & act & assert
            assertThatThrownBy {
                MemberModel(
                    loginId = "abc",
                    password = VALID_ENCODED_PASSWORD,
                    name = VALID_NAME,
                    birthDate = VALID_BIRTH_DATE,
                    email = VALID_EMAIL,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("loginId가 20자 초과이면 예외가 발생한다")
        @Test
        fun throwsException_whenLoginIdIsTooLong() {
            // arrange & act & assert
            assertThatThrownBy {
                MemberModel(
                    loginId = "a".repeat(21),
                    password = VALID_ENCODED_PASSWORD,
                    name = VALID_NAME,
                    birthDate = VALID_BIRTH_DATE,
                    email = VALID_EMAIL,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("loginId에 대문자가 포함되면 예외가 발생한다")
        @Test
        fun throwsException_whenLoginIdContainsUppercase() {
            // arrange & act & assert
            assertThatThrownBy {
                MemberModel(
                    loginId = "Test_user",
                    password = VALID_ENCODED_PASSWORD,
                    name = VALID_NAME,
                    birthDate = VALID_BIRTH_DATE,
                    email = VALID_EMAIL,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("loginId에 특수문자(언더스코어 제외)가 포함되면 예외가 발생한다")
        @Test
        fun throwsException_whenLoginIdContainsInvalidCharacters() {
            // arrange & act & assert
            assertThatThrownBy {
                MemberModel(
                    loginId = "test-user",
                    password = VALID_ENCODED_PASSWORD,
                    name = VALID_NAME,
                    birthDate = VALID_BIRTH_DATE,
                    email = VALID_EMAIL,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("이름 검증")
    @Nested
    inner class NameValidation {
        @DisplayName("이름이 빈 값이면 예외가 발생한다")
        @Test
        fun throwsException_whenNameIsEmpty() {
            // arrange & act & assert
            assertThatThrownBy {
                MemberModel(
                    loginId = VALID_LOGIN_ID,
                    password = VALID_ENCODED_PASSWORD,
                    name = "",
                    birthDate = VALID_BIRTH_DATE,
                    email = VALID_EMAIL,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("이름이 2자 미만이면 예외가 발생한다")
        @Test
        fun throwsException_whenNameIsTooShort() {
            // arrange & act & assert
            assertThatThrownBy {
                MemberModel(
                    loginId = VALID_LOGIN_ID,
                    password = VALID_ENCODED_PASSWORD,
                    name = "홍",
                    birthDate = VALID_BIRTH_DATE,
                    email = VALID_EMAIL,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("이름이 50자 초과이면 예외가 발생한다")
        @Test
        fun throwsException_whenNameIsTooLong() {
            // arrange & act & assert
            assertThatThrownBy {
                MemberModel(
                    loginId = VALID_LOGIN_ID,
                    password = VALID_ENCODED_PASSWORD,
                    name = "가".repeat(51),
                    birthDate = VALID_BIRTH_DATE,
                    email = VALID_EMAIL,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("이름에 숫자가 포함되면 예외가 발생한다")
        @Test
        fun throwsException_whenNameContainsNumbers() {
            // arrange & act & assert
            assertThatThrownBy {
                MemberModel(
                    loginId = VALID_LOGIN_ID,
                    password = VALID_ENCODED_PASSWORD,
                    name = "홍길동123",
                    birthDate = VALID_BIRTH_DATE,
                    email = VALID_EMAIL,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("이름에 특수문자가 포함되면 예외가 발생한다")
        @Test
        fun throwsException_whenNameContainsSpecialCharacters() {
            // arrange & act & assert
            assertThatThrownBy {
                MemberModel(
                    loginId = VALID_LOGIN_ID,
                    password = VALID_ENCODED_PASSWORD,
                    name = "홍길동!",
                    birthDate = VALID_BIRTH_DATE,
                    email = VALID_EMAIL,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("영문 이름은 허용된다")
        @Test
        fun allowsEnglishName() {
            // arrange & act
            val member = MemberModel(
                loginId = VALID_LOGIN_ID,
                password = VALID_ENCODED_PASSWORD,
                name = "John Doe",
                birthDate = VALID_BIRTH_DATE,
                email = VALID_EMAIL,
            )

            // assert
            assertThat(member.name).isEqualTo("John Doe")
        }
    }

    @DisplayName("이메일 검증")
    @Nested
    inner class EmailValidation {
        @DisplayName("이메일이 빈 값이면 예외가 발생한다")
        @Test
        fun throwsException_whenEmailIsEmpty() {
            // arrange & act & assert
            assertThatThrownBy {
                MemberModel(
                    loginId = VALID_LOGIN_ID,
                    password = VALID_ENCODED_PASSWORD,
                    name = VALID_NAME,
                    birthDate = VALID_BIRTH_DATE,
                    email = "",
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("이메일에 @가 없으면 예외가 발생한다")
        @Test
        fun throwsException_whenEmailHasNoAtSign() {
            // arrange & act & assert
            assertThatThrownBy {
                MemberModel(
                    loginId = VALID_LOGIN_ID,
                    password = VALID_ENCODED_PASSWORD,
                    name = VALID_NAME,
                    birthDate = VALID_BIRTH_DATE,
                    email = "testexample.com",
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("이메일 형식이 올바르지 않으면 예외가 발생한다")
        @Test
        fun throwsException_whenEmailFormatIsInvalid() {
            // arrange & act & assert
            assertThatThrownBy {
                MemberModel(
                    loginId = VALID_LOGIN_ID,
                    password = VALID_ENCODED_PASSWORD,
                    name = VALID_NAME,
                    birthDate = VALID_BIRTH_DATE,
                    email = "test@",
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("생년월일 검증")
    @Nested
    inner class BirthDateValidation {
        @DisplayName("생년월일이 미래이면 예외가 발생한다")
        @Test
        fun throwsException_whenBirthDateIsFuture() {
            // arrange & act & assert
            assertThatThrownBy {
                MemberModel(
                    loginId = VALID_LOGIN_ID,
                    password = VALID_ENCODED_PASSWORD,
                    name = VALID_NAME,
                    birthDate = LocalDate.now().plusDays(1),
                    email = VALID_EMAIL,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("생년월일이 오늘이면 예외가 발생하지 않는다")
        @Test
        fun allowsTodayAsBirthDate() {
            // arrange & act
            val member = MemberModel(
                loginId = VALID_LOGIN_ID,
                password = VALID_ENCODED_PASSWORD,
                name = VALID_NAME,
                birthDate = LocalDate.now(),
                email = VALID_EMAIL,
            )

            // assert
            assertThat(member.birthDate).isEqualTo(LocalDate.now())
        }
    }
}
