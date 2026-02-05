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

@SpringBootTest
class UserServiceIntegrationTest @Autowired constructor(
    private val userService: UserService,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("회원 가입시, ")
    @Nested
    inner class RegisterUser {
        @DisplayName("User 저장이 수행된다.")
        @Test
        fun saveUser_whenRegisterUser() {
            // arrange

            // act
            val result = userService.registerUser(loginId = "testId", password = "testPassword", name = "testName", birth = "2026-01-31", email = "test@test.com")

            // assert
            val user = userJpaRepository.findById(result.id).get()
            assertThat(user).isNotNull()
        }

        @DisplayName("이미 가입된 LoginId 로 시도하면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwConflictException_whenConflictIdIsProvided() {
            // arrange
            val user = userService.registerUser(loginId = "testId", password = "testPassword", name = "testName", birth = "2026-01-31", email = "test@test.com")

            // act
            val result = assertThrows<CoreException> {
                userService.registerUser(loginId = user.loginId, password = "testPassword", name = "testName", birth = "2026-01-31", email = "test@test.com")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.CONFLICT)
        }
    }

    @DisplayName("회원 정보 조회시, ")
    @Nested
    inner class GetUser {
        @DisplayName("해당 로그인 ID의 회원이 존재할 경우, 회원 정보가 반환된다.")
        @Test
        fun getUser_whenLoginIdExists() {
            // arrange
            val password = "abcd1234"
            val user = userJpaRepository.save(User(loginId = "testId", password = password, name = "testName", birth = "2026-01-31", email = "test@test.com"))

            // act
            val result = userService.getUserByLoginIdAndPassword(user.loginId, password)

            // assert
            assertAll(
                { assertThat(result).isNotNull() },
                { assertThat(result?.loginId).isEqualTo(user.loginId) },
                { assertThat(result?.name).isEqualTo(user.name) },
                { assertThat(result?.birth).isEqualTo(user.birth) },
                { assertThat(result?.email).isEqualTo(user.email) },
            )
        }

        @DisplayName("해당 로그인 ID의 회원이 존재하지 않을 경우, Null이 반환된다.")
        @Test
        fun returnNull_whenLoginIdNotExists() {
            // arrange
            val loginId = "testId"
            val password = "abcd1234"

            // act
            val result = userService.getUserByLoginIdAndPassword(loginId, password)

            // assert
            assertThat(result).isNull()
        }

        @DisplayName("해당 로그인 ID의 회원 비밀번호가 틀릴 경우, Null이 반환된다.")
        @Test
        fun returnNull_whenPasswordNotMatched() {
            // arrange
            val password = "abcd1234"
            val wrongPassword = "abcd1235"
            val user = userJpaRepository.save(User(loginId = "testId", password = password, name = "testName", birth = "2026-01-31", email = "test@test.com"))

            // act
            val result = userService.getUserByLoginIdAndPassword(user.loginId, wrongPassword)

            // assert
            assertThat(result).isNull()
        }
    }

    @DisplayName("비밀번호 수정시, ")
    @Nested
    inner class ChangePassword {
        @DisplayName("해당 로그인 ID의 회원이 존재하고 기존 비밀번호와 새 비밀번호가 유효한 경우, 비밀번호가 변경된다.")
        @Test
        fun changePassword_whenLoginIdExistsAndPasswordsAreValid() {
            // arrange
            val password = "abcd1234"
            val newPassword = "abcd1235"
            val user = userJpaRepository.save(User(loginId = "testId", password = password, name = "testName", birth = "2026-01-31", email = "test@test.com"))

            // act
            userService.chagePassword(user.loginId, password, newPassword)

            // assert
            val result = userJpaRepository.findByLoginId(user.loginId)
            assertAll(
                { assertThat(result).isNotNull() },
                { assertThat(result?.matchPassword(newPassword)).isTrue() },
            )
        }

        @DisplayName("해당 로그인 ID의 회원이 존재하고 기존 비밀번호가 일치하지만 새 비밀번호가 유효하지 않은 경우, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenNewPasswordIsInvalid() {
            // arrange
            val password = "abcd1234"
            val newPassword = "abcd"
            val user = userJpaRepository.save(User(loginId = "testId", password = password, name = "testName", birth = "2026-01-31", email = "test@test.com"))

            // act
            val result = assertThrows<CoreException> {
                userService.chagePassword(user.loginId, password, newPassword)
            }

            // assert
            val resultUser = userJpaRepository.findByLoginId(user.loginId)
            assertAll(
                { assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST) },
                { assertThat(resultUser).isNotNull() },
                { assertThat(resultUser?.matchPassword(password)).isTrue() },
                { assertThat(resultUser?.matchPassword(newPassword)).isFalse() },
            )
        }
    }
}
