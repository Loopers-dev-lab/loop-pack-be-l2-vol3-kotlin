package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.time.ZoneId
import java.time.ZonedDateTime

class UserModelTest {

    private val defaultUsername = Username.of("username")
    private val defaultPassword = Password.of("password1234!", DEFAULT_BIRTH_DATE)
    private val defaultName = "안유진"
    private val defaultEmail = Email.of("email@loopers.com")

    companion object {
        private val DEFAULT_BIRTH_DATE = ZonedDateTime.of(1995, 5, 29, 21, 40, 0, 0, ZoneId.of("Asia/Seoul"))
    }

    private fun createUserModel(
        username: Username = defaultUsername,
        password: Password = defaultPassword,
        name: String = defaultName,
        email: Email = defaultEmail,
        birthDate: ZonedDateTime = DEFAULT_BIRTH_DATE,
    ) = UserModel(
        username = username,
        password = password,
        name = name,
        email = email,
        birthDate = birthDate,
    )

    @DisplayName("생성")
    @Nested
    inner class Create {

        @DisplayName("유효한 파라미터가 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsUserModel_whenValidParametersAreProvided() {
            // act
            val userModel = createUserModel()

            // assert
            assertAll(
                { assertThat(userModel.id).isNotNull() },
                { assertThat(userModel.username).isEqualTo("username") },
                { assertThat(userModel.password).isEqualTo("password1234!") },
                { assertThat(userModel.name).isEqualTo(defaultName) },
                { assertThat(userModel.email).isEqualTo("email@loopers.com") },
                { assertThat(userModel.birthDate).isEqualTo(DEFAULT_BIRTH_DATE) },
            )
        }

        @DisplayName("파라미터 검증")
        @Nested
        inner class ParameterValidation {

            @DisplayName("이름이 비어있으면, BAD_REQUEST 예외가 발생한다.")
            @Test
            fun throwsBadRequestException_whenNameIsBlank() {
                // act
                val result = assertThrows<CoreException> {
                    createUserModel(name = "   ")
                }

                // assert
                assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            }

            @DisplayName("생년월일이 현재 시점 이후이면, BAD_REQUEST 예외가 발생한다.")
            @Test
            fun throwsBadRequestException_whenBirthDateIsInTheFuture() {
                // act
                val result = assertThrows<CoreException> {
                    createUserModel(birthDate = ZonedDateTime.now().plusYears(1))
                }

                // assert
                assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            }
        }
    }
}
