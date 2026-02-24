package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDate

/**
 * UserService 단위 테스트
 * - Mock을 사용하여 외부 의존성(Repository, PasswordEncoder) 격리
 * - 비밀번호 검증 및 암호화 로직 테스트
 *
 * 📌 Kotlin 설명: @ExtendWith(MockitoExtension::class)
 * - Java의 @RunWith(MockitoJUnitRunner.class)와 동일
 * - JUnit5에서는 @ExtendWith 사용
 */
@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    /**
     * 📌 Kotlin 설명: @Mock, @InjectMocks
     * - @Mock: 가짜 객체 생성 (Stub/Mock 역할 가능)
     * - @InjectMocks: Mock 객체들을 주입받아 테스트 대상 생성
     */
    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @InjectMocks
    private lateinit var userService: UserService

    @DisplayName("회원가입할 때,")
    @Nested
    inner class SignUp {

        @DisplayName("정상적인 정보가 주어지면, 회원이 생성된다.")
        @Test
        fun createsUser_whenValidInfoProvided() {
            // arrange
            val command = SignUpCommand(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            val encodedPassword = "encodedPassword123"

            whenever(userRepository.existsByLoginId(command.loginId)).thenReturn(false)
            whenever(passwordEncoder.encode(command.password)).thenReturn(encodedPassword)
            whenever(userRepository.save(any())).thenAnswer { it.arguments[0] }

            // act
            val result = userService.signUp(command)

            // assert
            assertThat(result.loginId).isEqualTo(command.loginId)
            assertThat(result.password).isEqualTo(encodedPassword)
        }

        @DisplayName("이미 존재하는 로그인 ID로 가입하면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsException_whenLoginIdAlreadyExists() {
            // arrange
            val command = SignUpCommand(
                loginId = "existingUser",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            whenever(userRepository.existsByLoginId(command.loginId)).thenReturn(true)

            // act
            val exception = assertThrows<CoreException> {
                userService.signUp(command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @DisplayName("비밀번호가 8자 미만이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenPasswordLessThan8Characters() {
            // arrange
            val command = SignUpCommand(
                loginId = "testuser1",
                password = "Pass1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            whenever(userRepository.existsByLoginId(command.loginId)).thenReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                userService.signUp(command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호가 16자를 초과하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenPasswordMoreThan16Characters() {
            // arrange
            val command = SignUpCommand(
                loginId = "testuser1",
                password = "Password1!Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            whenever(userRepository.existsByLoginId(command.loginId)).thenReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                userService.signUp(command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호에 생년월일(yyyyMMdd)이 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenPasswordContainsBirthDate() {
            // arrange
            val birthDate = LocalDate.of(1990, 1, 15)
            val command = SignUpCommand(
                loginId = "testuser1",
                password = "Pass19900115!",
                name = "홍길동",
                birthDate = birthDate,
                email = "test@example.com",
            )

            whenever(userRepository.existsByLoginId(command.loginId)).thenReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                userService.signUp(command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호에 허용되지 않는 문자가 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenPasswordContainsInvalidCharacter() {
            // arrange
            val command = SignUpCommand(
                loginId = "testuser1",
                password = "Password1! ",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            whenever(userRepository.existsByLoginId(command.loginId)).thenReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                userService.signUp(command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("내 정보를 조회할 때,")
    @Nested
    inner class GetMyInfo {

        @DisplayName("존재하는 회원 ID로 조회하면, 회원 정보가 반환된다.")
        @Test
        fun returnsUserInfo_whenUserExists() {
            // arrange
            val userId = 1L
            val user = User(
                loginId = "testuser1",
                password = "encodedPassword",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            whenever(userRepository.findById(userId)).thenReturn(user)

            // act
            val result = userService.getMyInfo(userId)

            // assert
            assertThat(result.loginId).isEqualTo("testuser1")
            assertThat(result.name).isEqualTo("홍길동")
            assertThat(result.email).isEqualTo("test@example.com")
        }

        @DisplayName("존재하지 않는 회원 ID로 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsException_whenUserNotFound() {
            // arrange
            val userId = 999L

            whenever(userRepository.findById(userId)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                userService.getMyInfo(userId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("비밀번호를 변경할 때,")
    @Nested
    inner class ChangePassword {

        @DisplayName("현재 비밀번호가 일치하면, 비밀번호가 변경된다.")
        @Test
        fun changesPassword_whenCurrentPasswordMatches() {
            // arrange
            val userId = 1L
            val currentPassword = "OldPassword1!"
            val newPassword = "NewPassword1!"
            val user = User(
                loginId = "testuser1",
                password = "encodedOldPassword",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            whenever(userRepository.findById(userId)).thenReturn(user)
            whenever(passwordEncoder.matches(currentPassword, user.password)).thenReturn(true)
            whenever(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword")
            whenever(userRepository.save(any())).thenAnswer { it.arguments[0] }

            // act
            userService.changePassword(userId, currentPassword, newPassword)

            // assert
            assertThat(user.password).isEqualTo("encodedNewPassword")
        }

        @DisplayName("현재 비밀번호가 일치하지 않으면, UNAUTHORIZED 예외가 발생한다.")
        @Test
        fun throwsException_whenCurrentPasswordDoesNotMatch() {
            // arrange
            val userId = 1L
            val currentPassword = "WrongPassword!"
            val newPassword = "NewPassword1!"
            val user = User(
                loginId = "testuser1",
                password = "encodedOldPassword",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            whenever(userRepository.findById(userId)).thenReturn(user)
            whenever(passwordEncoder.matches(currentPassword, user.password)).thenReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword(userId, currentPassword, newPassword)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }

        @DisplayName("존재하지 않는 회원 ID로 변경하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsException_whenUserNotFound() {
            // arrange
            val userId = 999L
            val currentPassword = "OldPassword1!"
            val newPassword = "NewPassword1!"

            whenever(userRepository.findById(userId)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword(userId, currentPassword, newPassword)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("새 비밀번호가 유효하지 않으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenNewPasswordIsInvalid() {
            // arrange
            val userId = 1L
            val currentPassword = "OldPassword1!"
            val newPassword = "short"
            val user = User(
                loginId = "testuser1",
                password = "encodedOldPassword",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            whenever(userRepository.findById(userId)).thenReturn(user)
            whenever(passwordEncoder.matches(currentPassword, user.password)).thenReturn(true)

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword(userId, currentPassword, newPassword)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("새 비밀번호가 현재 비밀번호와 동일하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenNewPasswordSameAsCurrent() {
            // arrange
            val userId = 1L
            val currentPassword = "SamePassword1!"
            val newPassword = "SamePassword1!"
            val user = User(
                loginId = "testuser1",
                password = "encodedPassword",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            whenever(userRepository.findById(userId)).thenReturn(user)
            whenever(passwordEncoder.matches(currentPassword, user.password)).thenReturn(true)

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword(userId, currentPassword, newPassword)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
