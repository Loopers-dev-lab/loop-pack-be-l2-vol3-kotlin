package com.loopers.application.user

import com.loopers.domain.user.PasswordEncoder
import com.loopers.domain.user.SignUpCommand
import com.loopers.domain.user.User
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
import java.time.LocalDate

/**
 * UserService 통합 테스트
 * - 실제 DB(TestContainers)와 연동하여 Service → Repository 레이어 통합 테스트
 * - @Transactional 경계가 Service에 있으므로 Service를 통해 테스트
 */
@SpringBootTest
class UserServiceIntegrationTest @Autowired constructor(
    private val userService: UserService,
    private val userJpaRepository: UserJpaRepository,
    private val passwordEncoder: PasswordEncoder,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("회원가입할 때,")
    @Nested
    inner class SignUp {

        @DisplayName("정상적인 정보가 주어지면, 회원이 DB에 저장된다.")
        @Test
        fun savesUserToDatabase_whenValidInfoProvided() {
            // arrange
            val command = SignUpCommand(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            // act
            val result = userService.signUp(command)

            // assert
            val savedUser = userJpaRepository.findByLoginId("testuser1")!!
            assertAll(
                { assertThat(savedUser.id).isEqualTo(result.id) },
                { assertThat(savedUser.loginId).isEqualTo("testuser1") },
                { assertThat(savedUser.name).isEqualTo("홍길동") },
                { assertThat(savedUser.email).isEqualTo("test@example.com") },
            )
        }

        @DisplayName("비밀번호가 암호화되어 저장된다.")
        @Test
        fun savesEncodedPassword_whenSignUp() {
            // arrange
            val rawPassword = "Password1!"
            val command = SignUpCommand(
                loginId = "testuser1",
                password = rawPassword,
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            // act
            userService.signUp(command)

            // assert
            val savedUser = userJpaRepository.findByLoginId("testuser1")!!
            assertAll(
                { assertThat(savedUser.password).isNotEqualTo(rawPassword) },
                { assertThat(passwordEncoder.matches(rawPassword, savedUser.password)).isTrue() },
            )
        }

        @DisplayName("중복된 loginId가 있으면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflict_whenDuplicateLoginId() {
            // arrange
            val existingUser = User(
                loginId = "testuser1",
                password = "encoded",
                name = "기존회원",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "existing@example.com",
            )
            userJpaRepository.save(existingUser)

            val command = SignUpCommand(
                loginId = "testuser1",
                password = "Password1!",
                name = "신규회원",
                birthDate = LocalDate.of(1995, 5, 5),
                email = "new@example.com",
            )

            // act & assert
            val exception = assertThrows<CoreException> {
                userService.signUp(command)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
        }
    }

    @DisplayName("내 정보 조회할 때,")
    @Nested
    inner class GetMyInfo {

        @DisplayName("존재하는 회원 ID가 주어지면, 회원 정보를 반환한다.")
        @Test
        fun returnsUserInfo_whenValidUserId() {
            // arrange
            val savedUser = userJpaRepository.save(
                User(
                    loginId = "testuser1",
                    password = "encoded",
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 15),
                    email = "test@example.com",
                ),
            )

            // act
            val result = userService.getMyInfo(savedUser.id)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(savedUser.id) },
                { assertThat(result.loginId).isEqualTo("testuser1") },
                { assertThat(result.name).isEqualTo("홍길동") },
                { assertThat(result.email).isEqualTo("test@example.com") },
            )
        }

        @DisplayName("존재하지 않는 회원 ID가 주어지면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenUserNotExists() {
            // arrange
            val nonExistentId = 9999L

            // act & assert
            val exception = assertThrows<CoreException> {
                userService.getMyInfo(nonExistentId)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("비밀번호 변경할 때,")
    @Nested
    inner class ChangePassword {

        @DisplayName("정상적인 요청이면, 비밀번호가 변경되어 DB에 저장된다.")
        @Test
        fun changesPasswordInDatabase_whenValidRequest() {
            // arrange
            val currentPassword = "OldPassword1!"
            val newPassword = "NewPassword1!"
            val savedUser = userJpaRepository.save(
                User(
                    loginId = "testuser1",
                    password = passwordEncoder.encode(currentPassword),
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 15),
                    email = "test@example.com",
                ),
            )

            // act
            userService.changePassword(savedUser.id, currentPassword, newPassword)

            // assert
            val updatedUser = userJpaRepository.findById(savedUser.id).get()
            assertAll(
                { assertThat(passwordEncoder.matches(newPassword, updatedUser.password)).isTrue() },
                { assertThat(passwordEncoder.matches(currentPassword, updatedUser.password)).isFalse() },
            )
        }

        @DisplayName("현재 비밀번호가 일치하지 않으면, UNAUTHORIZED 예외가 발생한다.")
        @Test
        fun throwsUnauthorized_whenCurrentPasswordMismatch() {
            // arrange
            val savedUser = userJpaRepository.save(
                User(
                    loginId = "testuser1",
                    password = passwordEncoder.encode("CorrectPassword1!"),
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 15),
                    email = "test@example.com",
                ),
            )

            // act & assert
            val exception = assertThrows<CoreException> {
                userService.changePassword(savedUser.id, "WrongPassword1!", "NewPassword1!")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }

        @DisplayName("새 비밀번호가 현재 비밀번호와 같으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNewPasswordSameAsCurrent() {
            // arrange
            val currentPassword = "SamePassword1!"
            val savedUser = userJpaRepository.save(
                User(
                    loginId = "testuser1",
                    password = passwordEncoder.encode(currentPassword),
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 15),
                    email = "test@example.com",
                ),
            )

            // act & assert
            val exception = assertThrows<CoreException> {
                userService.changePassword(savedUser.id, currentPassword, currentPassword)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
