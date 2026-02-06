package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class UserServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        userService = UserService(userRepository)
    }

    @Nested
    @DisplayName("회원가입 시")
    inner class SignUp {

        @Test
        @DisplayName("유효한 정보로 가입하면 User가 저장되고 반환된다")
        fun signUp_withValidData_savesAndReturnsUser() {
            // arrange
            val command = UserCommand.SignUp(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            val userSlot = slot<User>()
            every { userRepository.existsByLoginId(command.loginId) } returns false
            every { userRepository.save(capture(userSlot)) } answers { userSlot.captured }

            // act
            val result = userService.signUp(command)

            // assert
            assertThat(result.loginId).isEqualTo("testuser1")
            assertThat(result.name).isEqualTo("홍길동")
            assertThat(result.email).isEqualTo("test@example.com")
            verify(exactly = 1) { userRepository.existsByLoginId(command.loginId) }
            verify(exactly = 1) { userRepository.save(any()) }
        }

        @Test
        @DisplayName("이미 존재하는 로그인 ID로 가입하면 CONFLICT 예외가 발생한다")
        fun signUp_duplicateLoginId_throwsConflict() {
            // arrange
            val command = UserCommand.SignUp(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            every { userRepository.existsByLoginId(command.loginId) } returns true

            // act
            val exception = assertThrows<CoreException> {
                userService.signUp(command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
            assertThat(exception.message).isEqualTo("이미 존재하는 로그인 ID입니다.")
            verify(exactly = 1) { userRepository.existsByLoginId(command.loginId) }
            verify(exactly = 0) { userRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("내 정보 조회 시")
    inner class GetUserInfo {

        @Test
        @DisplayName("존재하는 사용자를 조회하면 User를 반환한다")
        fun getUserInfo_userExists_returnsUser() {
            // arrange
            val user = User(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            every { userRepository.findByLoginId("testuser1") } returns user

            // act
            val result = userService.getUserInfo("testuser1")

            // assert
            assertThat(result).isNotNull()
            assertThat(result?.loginId).isEqualTo("testuser1")
            verify(exactly = 1) { userRepository.findByLoginId("testuser1") }
        }

        @Test
        @DisplayName("존재하지 않는 사용자를 조회하면 null을 반환한다")
        fun getUserInfo_userNotExists_returnsNull() {
            // arrange
            every { userRepository.findByLoginId("nonexistent") } returns null

            // act
            val result = userService.getUserInfo("nonexistent")

            // assert
            assertThat(result).isNull()
            verify(exactly = 1) { userRepository.findByLoginId("nonexistent") }
        }
    }

    @Nested
    @DisplayName("비밀번호 변경 시")
    inner class ChangePassword {

        @Test
        @DisplayName("유효한 요청이면 비밀번호가 변경된다")
        fun changePassword_withValidRequest_success() {
            // arrange
            val user = User(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            every { userRepository.findByLoginId("testuser1") } returns user

            val command = UserCommand.ChangePassword(
                currentPassword = "Password1!",
                newPassword = "NewPass12!",
            )

            // act
            userService.changePassword("testuser1", command)

            // assert
            assertThat(user.verifyPassword("NewPass12!")).isTrue()
        }

        @Test
        @DisplayName("존재하지 않는 사용자이면 NOT_FOUND 예외가 발생한다")
        fun changePassword_userNotFound_throwsNotFound() {
            // arrange
            every { userRepository.findByLoginId("nonexistent") } returns null

            val command = UserCommand.ChangePassword(
                currentPassword = "Password1!",
                newPassword = "NewPass12!",
            )

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword("nonexistent", command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
            assertThat(exception.message).isEqualTo("사용자를 찾을 수 없습니다.")
        }

        @Test
        @DisplayName("현재 비밀번호가 일치하지 않으면 BAD_REQUEST 예외가 발생한다")
        fun changePassword_wrongCurrentPassword_throwsBadRequest() {
            // arrange
            val user = User(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            every { userRepository.findByLoginId("testuser1") } returns user

            val command = UserCommand.ChangePassword(
                currentPassword = "WrongPass1!",
                newPassword = "NewPass12!",
            )

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword("testuser1", command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("현재 비밀번호가 일치하지 않습니다.")
        }

        @Test
        @DisplayName("현재 비밀번호와 동일한 비밀번호로 변경하면 BAD_REQUEST 예외가 발생한다")
        fun changePassword_sameAsCurrent_throwsBadRequest() {
            // arrange
            val user = User(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            every { userRepository.findByLoginId("testuser1") } returns user

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
        @DisplayName("새 비밀번호가 8자 미만이면 BAD_REQUEST 예외가 발생한다")
        fun changePassword_tooShort_throwsBadRequest() {
            // arrange
            val user = User(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            every { userRepository.findByLoginId("testuser1") } returns user

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
        @DisplayName("새 비밀번호가 16자 초과하면 BAD_REQUEST 예외가 발생한다")
        fun changePassword_tooLong_throwsBadRequest() {
            // arrange
            val user = User(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            every { userRepository.findByLoginId("testuser1") } returns user

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
        @DisplayName("새 비밀번호에 생년월일이 포함되면 BAD_REQUEST 예외가 발생한다")
        fun changePassword_containsBirthDate_throwsBadRequest() {
            // arrange
            val user = User(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            every { userRepository.findByLoginId("testuser1") } returns user

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
        @DisplayName("새 비밀번호에 동일 문자가 3회 이상 연속되면 BAD_REQUEST 예외가 발생한다")
        fun changePassword_consecutiveChars_throwsBadRequest() {
            // arrange
            val user = User(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            every { userRepository.findByLoginId("testuser1") } returns user

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
