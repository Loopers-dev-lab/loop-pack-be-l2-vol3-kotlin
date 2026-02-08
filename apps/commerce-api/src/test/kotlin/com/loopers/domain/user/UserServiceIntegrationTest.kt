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
import java.time.LocalDate

@SpringBootTest
class UserServiceIntegrationTest @Autowired constructor(
    private val userService: UserService,
    private val databaseCleanUp: DatabaseCleanUp,
    private val userJpaRepository: UserJpaRepository,
) {
    @AfterEach
    fun tearDown() = databaseCleanUp.truncateAllTables()

    @Nested
    @DisplayName("회원가입 시")
    inner class SignUp {
        @Test
        @DisplayName("중복 ID면 실패한다")
        fun signUp_duplicateId_throwsException() {
            val command = UserCommand.SignUp(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "existing@example.com",
            )

            userService.signUp(command)

            // 같은 ID로 다시 가입 시도
            val exception = assertThrows<CoreException> {
                userService.signUp(command)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @Test
        @DisplayName("성공하면 User가 저장된다")
        fun signUp_success() {
            val command = UserCommand.SignUp(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            val user = userService.signUp(command)

            assertThat(user.loginId).isEqualTo("testuser1")
        }
    }

    @Nested
    @DisplayName("내 정보 조회 시,")
    inner class GetUserInfo {

        @Test
        @DisplayName("해당 ID의 회원이 존재할 경우, 회원 정보가 반환된다.")
        fun returnsUserInfo_whenUserExists() {
            // arrange
            val user = UserCommand.SignUp(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            userService.signUp(user)

            // act
            val result = userService.getUserInfo("testuser1")

            // assert
            assertAll(
                { assertThat(result).isNotNull() },
                { assertThat(result?.loginId).isEqualTo("testuser1") },
                { assertThat(result?.name).isEqualTo("홍길동") },
            )
        }

        @Test
        @DisplayName("해당 ID의 회원이 존재하지 않을 경우, null이 반환된다.")
        fun returnsNull_whenUserDoesNotExist() {
            // arrange
            val nonExistentLoginId = "nonexistent"

            // act
            val result = userService.getUserInfo(nonExistentLoginId)

            // assert
            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("비밀번호 변경 시,")
    inner class ChangePassword {

        @Test
        @DisplayName("비밀번호 변경이 성공하면, 새 비밀번호로 인증할 수 있다.")
        fun changesPassword_whenRequestIsValid() {
            // arrange
            val user = UserCommand.SignUp(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            userService.signUp(user)

            val command = UserCommand.ChangePassword(
                currentPassword = "Password1!",
                newPassword = "NewPassword1!",
            )

            // act
            userService.changePassword("testuser1", command)

            // assert
            val updatedUser = userJpaRepository.findByLoginId("testuser1")
            assertThat(updatedUser?.verifyPassword("NewPassword1!")).isTrue()
        }

        @Test
        @DisplayName("존재하지 않는 사용자의 비밀번호를 변경하려 하면, 예외가 발생한다.")
        fun throwsException_whenUserDoesNotExist() {
            // arrange
            val command = UserCommand.ChangePassword(
                currentPassword = "Password1!",
                newPassword = "NewPassword1!",
            )

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword("nonexistent", command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("현재 비밀번호가 일치하지 않으면, 예외가 발생한다.")
        fun throwsException_whenCurrentPasswordIsWrong() {
            // arrange
            val user = UserCommand.SignUp(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            userService.signUp(user)

            val command = UserCommand.ChangePassword(
                currentPassword = "WrongPassword!",
                newPassword = "NewPassword1!",
            )

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword("testuser1", command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("현재 비밀번호와 동일한 비밀번호로 변경하면, 예외가 발생한다.")
        fun throwsException_whenNewPasswordIsSameAsCurrent() {
            // arrange
            val user = UserCommand.SignUp(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            userService.signUp(user)

            val command = UserCommand.ChangePassword(
                currentPassword = "Password1!",
                newPassword = "Password1!",
            )

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword("testuser1", command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("현재 비밀번호와 동일한 비밀번호는 사용할 수 없습니다.")
        }

        @Test
        @DisplayName("새 비밀번호가 8자 미만이면, 예외가 발생한다.")
        fun throwsException_whenNewPasswordIsTooShort() {
            // arrange
            val user = UserCommand.SignUp(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            userService.signUp(user)

            val command = UserCommand.ChangePassword(
                currentPassword = "Password1!",
                newPassword = "Short1!",
            )

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword("testuser1", command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("새 비밀번호가 16자 초과하면, 예외가 발생한다.")
        fun throwsException_whenNewPasswordIsTooLong() {
            // arrange
            val user = UserCommand.SignUp(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            userService.signUp(user)

            val command = UserCommand.ChangePassword(
                currentPassword = "Password1!",
                newPassword = "Password12345678!",
            )

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword("testuser1", command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("새 비밀번호에 생년월일이 포함되면, 예외가 발생한다.")
        fun throwsException_whenNewPasswordContainsBirthDate() {
            // arrange
            val user = UserCommand.SignUp(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            userService.signUp(user)

            val command = UserCommand.ChangePassword(
                currentPassword = "Password1!",
                newPassword = "Pass19900115!",
            )

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword("testuser1", command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("비밀번호에 생년월일을 포함할 수 없습니다.")
        }

        @Test
        @DisplayName("새 비밀번호에 동일 문자가 3회 이상 연속되면, 예외가 발생한다.")
        fun throwsException_whenNewPasswordHasConsecutiveChars() {
            // arrange
            val user = UserCommand.SignUp(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            userService.signUp(user)

            val command = UserCommand.ChangePassword(
                currentPassword = "Password1!",
                newPassword = "Passsword1!",
            )

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword("testuser1", command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("동일 문자가 3회 이상 연속될 수 없습니다.")
        }
    }
}
