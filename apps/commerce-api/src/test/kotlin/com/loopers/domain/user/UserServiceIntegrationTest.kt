package com.loopers.domain.user

import com.loopers.domain.user.PasswordEncryptor
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

@SpringBootTest
class UserServiceIntegrationTest @Autowired constructor(
    private val userService: UserService,
    private val userJpaRepository: UserJpaRepository,
    private val passwordEncryptor: PasswordEncryptor,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("사용자 생성 시,")
    @Nested
    inner class CreateUser {

        @Test
        fun `유효한 정보로 생성하면 성공한다`() {
            val loginId = "test1234"
            val password = "Test1234!@#$"
            val name = "loopers"
            val birthDate = "2000-01-01"
            val email = "test1234@loopers.com"
            val encryptedPassword = passwordEncryptor.encrypt(password)

            val user = userService.createUser(loginId, encryptedPassword, name, birthDate, email)

            assertAll(
                { assertThat(user.id).isGreaterThan(0) },
                { assertThat(user.loginId.value).isEqualTo(loginId) },
                { assertThat(user.password).isNotEqualTo(password) },
                { assertThat(passwordEncryptor.matches(password, user.password)).isTrue() },
                { assertThat(user.name.value).isEqualTo(name) },
                { assertThat(user.birthDate.value).isEqualTo(birthDate) },
                { assertThat(user.email.value).isEqualTo(email) },
            )
        }
    }

    @DisplayName("사용자 조회 시,")
    @Nested
    inner class GetUser {

        @Test
        fun `존재하는 LoginId로 조회하면 성공한다`() {
            val user = userService.createUser(
                "test1234",
                passwordEncryptor.encrypt("Test1234!@#$"),
                "loopers",
                "2000-01-01",
                "test1234@loopers.com",
            )

            val found = userService.getUserByLoginId(user.loginId.value)

            found?.let { assertThat(it.loginId) }?.isEqualTo(user.loginId)
        }

        @Test
        fun `존재하지 않는 LoginId로 조회하면 NOT_FOUND 예외가 발생한다`() {
            val exception = assertThrows<CoreException> {
                userService.getUserByLoginId("test1234")
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("비밀번호 수정 시,")
    @Nested
    inner class UpdatePassword {

        @Test
        fun `유효한 비밀번호로 수정하면 성공한다`() {
            val user = userService.createUser(
                "test1234",
                passwordEncryptor.encrypt("Test1234!@#$"),
                "loopers",
                "2000-01-01",
                "test1234@loopers.com",
            )
            val newPassword = "Newpass1234!@#$"

            userService.updatePassword(user.loginId.value, passwordEncryptor.encrypt(newPassword))

            val updated = userService.getUserByLoginId(user.loginId.value)
            updated?.let { assertThat(passwordEncryptor.matches(newPassword, it.password)) }?.isTrue()
        }
    }
}
