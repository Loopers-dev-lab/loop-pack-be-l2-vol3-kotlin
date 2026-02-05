package com.loopers.domain.user

import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.ZoneId
import java.time.ZonedDateTime

@SpringBootTest
class UserServiceIntegrationTest @Autowired constructor(
    private val userService: UserService,
    private val userJpaRepository: UserJpaRepository,
    private val passwordEncoder: PasswordEncoder,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val DEFAULT_USERNAME = "username"
        private const val DEFAULT_PASSWORD = "password1234!"
        private const val DEFAULT_NAME = "안유진"
        private const val DEFAULT_EMAIL = "email@loopers.com"
        private val DEFAULT_BIRTH_DATE = ZonedDateTime.of(1995, 5, 29, 21, 40, 0, 0, ZoneId.of("Asia/Seoul"))
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createRegisterCommand(
        username: String = DEFAULT_USERNAME,
        password: String = DEFAULT_PASSWORD,
        name: String = DEFAULT_NAME,
        email: String = DEFAULT_EMAIL,
        birthDate: ZonedDateTime = DEFAULT_BIRTH_DATE,
    ): RegisterCommand = RegisterCommand(
        username = username,
        password = password,
        name = name,
        email = email,
        birthDate = birthDate,
    )

    @DisplayName("회원가입")
    @Nested
    inner class Register {
        @DisplayName("유효한 정보가 주어지면, 암호화된 비밀번호로 저장된다.")
        @Test
        fun registersUser_whenValidInfoIsProvided() {
            // arrange
            val userModel = createRegisterCommand()

            // act
            val result = userService.register(userModel)

            // assert
            assertAll(
                { assertThat(result.id).isNotNull() },
                { assertThat(result.username).isEqualTo(DEFAULT_USERNAME) },
                { assertThat(result.name).isEqualTo(DEFAULT_NAME) },
                { assertThat(result.email).isEqualTo(DEFAULT_EMAIL) },
                { assertThat(passwordEncoder.matches(DEFAULT_PASSWORD, result.password)).isTrue() },
            )
        }

        @DisplayName("중복된 아이디가 주어지면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflictException_whenDuplicateUsernameIsProvided() {
            // arrange
            userService.register(createRegisterCommand())
            val duplicatedUser = createRegisterCommand()

            // act
            val result = assertThrows<CoreException> {
                userService.register(duplicatedUser)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.CONFLICT)
        }
    }

    @DisplayName("조회")
    @Nested
    inner class GetUser {
        @DisplayName("존재하는 username이 주어지면, 해당 유저 정보를 반환한다.")
        @Test
        fun returnsUserModel_whenValidUsernameIsProvided() {
            // arrange
            val registered = userService.register(createRegisterCommand())

            // act
            val result = userService.getUser(registered.username)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(registered.id) },
                { assertThat(result.username).isEqualTo(DEFAULT_USERNAME) },
                { assertThat(result.name).isEqualTo(DEFAULT_NAME) },
                { assertThat(result.email).isEqualTo(DEFAULT_EMAIL) },
            )
        }

        @DisplayName("존재하지 않는 username이 주어지면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFoundException_whenInvalidUsernameIsProvided() {
            // arrange
            val invalidUsername = "nonexistent"

            // act
            val result = assertThrows<CoreException> {
                userService.getUser(invalidUsername)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("비밀번호 수정")
    @Nested
    inner class UpdatePassword {
        @DisplayName("유효한 기존 비밀번호와 새 비밀번호가 주어지면, 비밀번호가 변경되고 암호화 저장된다.")
        @Test
        fun updatesPassword_whenValidPasswordsAreProvided() {
            // arrange
            val registered = userService.register(createRegisterCommand())
            val newPassword = "newPassword1!"
            val command = UpdatePasswordCommand(registered.username, DEFAULT_PASSWORD, newPassword)

            // act
            userService.updatePassword(command)

            // assert
            val updatedUser = userJpaRepository.findById(registered.id).get()
            assertAll(
                { assertThat(passwordEncoder.matches(newPassword, updatedUser.password)).isTrue() },
                { assertThat(passwordEncoder.matches(DEFAULT_PASSWORD, updatedUser.password)).isFalse() },
            )
        }

        @DisplayName("새 비밀번호가 기존과 동일하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenNewPasswordIsSameAsCurrent() {
            // arrange
            val registered = userService.register(createRegisterCommand())
            val command = UpdatePasswordCommand(registered.username, DEFAULT_PASSWORD, DEFAULT_PASSWORD)

            // act
            val result = assertThrows<CoreException> {
                userService.updatePassword(command)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
