package com.loopers.domain.user

import com.loopers.domain.user.dto.SignUpCommand
import com.loopers.domain.user.dto.UserInfo
import com.loopers.domain.user.vo.BirthDate
import com.loopers.domain.user.vo.Email
import com.loopers.domain.user.vo.LoginId
import com.loopers.domain.user.vo.Name
import com.loopers.domain.user.vo.Password
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder

class UserServiceTest {

    private lateinit var userService: UserService
    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        passwordEncoder = mockk()
        userService = UserService(userRepository, passwordEncoder)
    }

    @DisplayName("회원가입을 할 수 있다")
    @Test
    fun signUp() {
        // given
        val command = SignUpCommand(
            loginId = "test123",
            password = "test1234",
            name = "테스트",
            birthDate = "20260101",
            email = "test@test.com",
        )

        every { userRepository.existsByLoginId(any()) } returns false
        every { userRepository.save(any()) } answers { firstArg() }
        every { passwordEncoder.encode(any()) } returns "encodedPassword"

        // when + then
        userService.signUp(command)

        verify { userRepository.save(any()) }
    }

    @DisplayName("중복된 로그인ID로 회원가입을 할 수 없다")
    @Test
    fun signUpWithDuplicateLoginId() {
        // given
        val command = SignUpCommand(
            loginId = "test123",
            password = "test1234",
            name = "테스트",
            birthDate = "20260101",
            email = "test@test.com",
        )

        every { userRepository.existsByLoginId(any()) } returns true

        // when + then
        assertThatThrownBy {
            userService.signUp(command)
        }.isInstanceOf(CoreException::class.java)
            .extracting { (it as CoreException).errorType }
            .isEqualTo(ErrorType.BAD_REQUEST)
    }

    @DisplayName("회원가입 시 비밀번호가 8자 미만이면 예외가 발생한다")
    @Test
    fun signUpWithPasswordTooShort() {
        // given
        val command = SignUpCommand(
            loginId = "test123",
            password = "short1",
            name = "테스트",
            birthDate = "20260101",
            email = "test@test.com",
        )

        every { userRepository.existsByLoginId(any()) } returns false

        // when + then
        assertThatThrownBy {
            userService.signUp(command)
        }.isInstanceOf(CoreException::class.java)
            .extracting { (it as CoreException).errorType }
            .isEqualTo(ErrorType.BAD_REQUEST)
    }

    @DisplayName("회원가입 시 비밀번호가 16자 초과면 예외가 발생한다")
    @Test
    fun signUpWithPasswordTooLong() {
        // given
        val command = SignUpCommand(
            loginId = "test123",
            password = "thisPasswordIsTooLong123",
            name = "테스트",
            birthDate = "20260101",
            email = "test@test.com",
        )

        every { userRepository.existsByLoginId(any()) } returns false

        // when + then
        assertThatThrownBy {
            userService.signUp(command)
        }.isInstanceOf(CoreException::class.java)
            .extracting { (it as CoreException).errorType }
            .isEqualTo(ErrorType.BAD_REQUEST)
    }

    @DisplayName("회원가입 시 비밀번호에 허용되지 않는 문자가 포함되면 예외가 발생한다")
    @Test
    fun signUpWithInvalidPasswordCharacters() {
        // given
        val command = SignUpCommand(
            loginId = "test123",
            password = "pass한글포함123",
            name = "테스트",
            birthDate = "20260101",
            email = "test@test.com",
        )

        every { userRepository.existsByLoginId(any()) } returns false

        // when + then
        assertThatThrownBy {
            userService.signUp(command)
        }.isInstanceOf(CoreException::class.java)
            .extracting { (it as CoreException).errorType }
            .isEqualTo(ErrorType.BAD_REQUEST)
    }

    @DisplayName("회원가입 시 비밀번호에 생년월일이 포함되면 예외가 발생한다")
    @Test
    fun signUpWithBirthDateInPassword() {
        // given
        val command = SignUpCommand(
            loginId = "test123",
            password = "pass20260101!",
            name = "테스트",
            birthDate = "20260101",
            email = "test@test.com",
        )

        every { userRepository.existsByLoginId(any()) } returns false

        // when + then
        assertThatThrownBy {
            userService.signUp(command)
        }.isInstanceOf(CoreException::class.java)
            .extracting { (it as CoreException).errorType }
            .isEqualTo(ErrorType.BAD_REQUEST)
    }

    @DisplayName("내 정보를 조회할 수 있다")
    @Test
    fun findUserInfo() {
        // given
        val id = 1L

        val expectedUser = mockk<User> {
            every { loginId } returns LoginId.of("test123")
            every { name } returns Name.of("테스트")
            every { birthDate } returns BirthDate.of("20260101")
            every { email } returns Email.of("test@test.com")
        }

        val expectedUserInfo = UserInfo.from(expectedUser)

        every { userRepository.findUserById(any()) } returns expectedUser

        // when
        val userInfo = userService.findUserInfo(id)

        // then
        assertEquals(expectedUserInfo.loginId, userInfo.loginId)
        assertEquals(expectedUserInfo.name, userInfo.name)
        assertEquals(expectedUserInfo.birthDate, userInfo.birthDate)
        assertEquals(expectedUserInfo.email, userInfo.email)
    }

    @DisplayName("내 정보 조회 시 고객정보가 없으면 NotFound Exception이 발생한다")
    @Test
    fun findUserInfoNotFound() {
        // given
        val id = 1L
        every { userRepository.findUserById(any()) } returns null

        // when + then
        assertThatThrownBy {
            userService.findUserInfo(id)
        }.isInstanceOf(CoreException::class.java)
            .extracting { (it as CoreException).errorType }
            .isEqualTo(ErrorType.NOT_FOUND)
    }

    @DisplayName("비밀번호를 변경할 수 있다")
    @Test
    fun changePassword() {
        // given
        val id = 1L
        val currentPassword = "oldPass123"
        val newPassword = "newPass123"
        val encodedCurrentPassword = "encodedOldPass"

        val existingUser = mockk<User>(relaxed = true) {
            every { password } returns Password("encodedOldPass")
            every { birthDate } returns BirthDate.of("20260101")
        }

        every { userRepository.findUserById(id) } returns existingUser
        every { passwordEncoder.matches(currentPassword, encodedCurrentPassword) } returns true
        every { passwordEncoder.matches(newPassword, encodedCurrentPassword) } returns false
        every { passwordEncoder.encode(newPassword) } returns "encodedNewPass"
        every { userRepository.save(any()) } returns existingUser

        // when
        userService.changePassword(id, currentPassword, newPassword)

        // then
        verify { existingUser.changePassword(any()) }
    }

    @DisplayName("비밀번호 변경 시 기존 비밀번호가 일치하지 않으면 예외가 발생한다")
    @Test
    fun changePasswordWithIncorrectCurrentPassword() {
        // given
        val id = 1L
        val currentPassword = "wrongPassword"
        val newPassword = "newPass123"
        val encodedCurrentPassword = "encodedOldPass"

        val existingUser = mockk<User> {
            every { password } returns Password("encodedOldPass")
        }

        every { userRepository.findUserById(id) } returns existingUser
        every { passwordEncoder.matches(currentPassword, encodedCurrentPassword) } returns false

        // when + then
        assertThatThrownBy {
            userService.changePassword(id, currentPassword, newPassword)
        }.isInstanceOf(CoreException::class.java)
            .extracting { (it as CoreException).errorType }
            .isEqualTo(ErrorType.BAD_REQUEST)
    }

    @DisplayName("비밀번호 변경 시 새 비밀번호가 기존과 같으면 예외가 발생한다")
    @Test
    fun changePasswordWithSamePassword() {
        // given
        val id = 1L
        val currentPassword = "samePass123"
        val newPassword = "samePass123"
        val encodedPassword = "encodedSamePass"

        val existingUser = mockk<User> {
            every { password } returns Password("encodedSamePass")
        }

        every { userRepository.findUserById(id) } returns existingUser
        every { passwordEncoder.matches(currentPassword, encodedPassword) } returns true
        every { passwordEncoder.matches(newPassword, encodedPassword) } returns true

        // when + then
        assertThatThrownBy {
            userService.changePassword(id, currentPassword, newPassword)
        }.isInstanceOf(CoreException::class.java)
            .extracting { (it as CoreException).errorType }
            .isEqualTo(ErrorType.BAD_REQUEST)
    }

    @DisplayName("비밀번호 변경 시 새 비밀번호에 생년월일이 포함되면 예외가 발생한다")
    @Test
    fun changePasswordWithBirthDateInNewPassword() {
        // given
        val id = 1L
        val currentPassword = "oldPass123"
        val birthDate = "20260101"
        val newPassword = "pass$birthDate!"
        val encodedCurrentPassword = "encodedOldPass"

        val existingUser = mockk<User> {
            every { password } returns Password("encodedOldPass")
            every { this@mockk.birthDate } returns BirthDate.of("20260101")
        }

        every { userRepository.findUserById(id) } returns existingUser
        every { passwordEncoder.matches(currentPassword, encodedCurrentPassword) } returns true
        every { passwordEncoder.matches(newPassword, encodedCurrentPassword) } returns false

        // when + then
        assertThatThrownBy {
            userService.changePassword(id, currentPassword, newPassword)
        }.isInstanceOf(CoreException::class.java)
            .extracting { (it as CoreException).errorType }
            .isEqualTo(ErrorType.BAD_REQUEST)
    }

    @DisplayName("비밀번호 변경 시 새 비밀번호가 8자 미만이면 예외가 발생한다")
    @Test
    fun changePasswordWithNewPasswordTooShort() {
        // given
        val id = 1L
        val currentPassword = "oldPass123"
        val newPassword = "short1"
        val encodedCurrentPassword = "encodedOldPass"

        val existingUser = mockk<User> {
            every { password } returns Password("encodedOldPass")
            every { birthDate } returns BirthDate.of("20260101")
        }

        every { userRepository.findUserById(id) } returns existingUser
        every { passwordEncoder.matches(currentPassword, encodedCurrentPassword) } returns true
        every { passwordEncoder.matches(newPassword, encodedCurrentPassword) } returns false

        // when + then
        assertThatThrownBy {
            userService.changePassword(id, currentPassword, newPassword)
        }.isInstanceOf(CoreException::class.java)
            .extracting { (it as CoreException).errorType }
            .isEqualTo(ErrorType.BAD_REQUEST)
    }

    @DisplayName("비밀번호 변경 시 새 비밀번호가 16자 초과면 예외가 발생한다")
    @Test
    fun changePasswordWithNewPasswordTooLong() {
        // given
        val id = 1L
        val currentPassword = "oldPass123"
        val newPassword = "thisPasswordIsTooLong123"
        val encodedCurrentPassword = "encodedOldPass"

        val existingUser = mockk<User> {
            every { password } returns Password("encodedOldPass")
            every { birthDate } returns BirthDate.of("20260101")
        }

        every { userRepository.findUserById(id) } returns existingUser
        every { passwordEncoder.matches(currentPassword, encodedCurrentPassword) } returns true
        every { passwordEncoder.matches(newPassword, encodedCurrentPassword) } returns false

        // when + then
        assertThatThrownBy {
            userService.changePassword(id, currentPassword, newPassword)
        }.isInstanceOf(CoreException::class.java)
            .extracting { (it as CoreException).errorType }
            .isEqualTo(ErrorType.BAD_REQUEST)
    }

    @DisplayName("비밀번호 변경 시 새 비밀번호에 허용되지 않는 문자가 포함되면 예외가 발생한다")
    @Test
    fun changePasswordWithInvalidCharactersInNewPassword() {
        // given
        val id = 1L
        val currentPassword = "oldPass123"
        val newPassword = "pass한글포함123"
        val encodedCurrentPassword = "encodedOldPass"

        val existingUser = mockk<User> {
            every { password } returns Password("encodedOldPass")
            every { birthDate } returns BirthDate.of("20260101")
        }

        every { userRepository.findUserById(id) } returns existingUser
        every { passwordEncoder.matches(currentPassword, encodedCurrentPassword) } returns true
        every { passwordEncoder.matches(newPassword, encodedCurrentPassword) } returns false

        // when + then
        assertThatThrownBy {
            userService.changePassword(id, currentPassword, newPassword)
        }.isInstanceOf(CoreException::class.java)
            .extracting { (it as CoreException).errorType }
            .isEqualTo(ErrorType.BAD_REQUEST)
    }
}
