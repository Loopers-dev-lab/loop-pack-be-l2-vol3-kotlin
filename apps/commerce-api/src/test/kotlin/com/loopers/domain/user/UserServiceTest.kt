package com.loopers.domain.user

import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate


@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest @Autowired constructor(
    private val userService: UserService,
    private val databaseCleanUp: DatabaseCleanUp,
    private val passwordEncoder: PasswordEncoder
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    fun `createUser() should create user with valid data`() {
        // Arrange
        val userId = "testId"
        val password = "testPassword"
        val name = "testName"
        val birthDate = LocalDate.now()
        val email = "test@email.com"

        // Act
        val user = userService.createUser(userId, password, name, birthDate, email)

        // Assert
        assertThat(user.id).isGreaterThan(0)
        assertThat(user.userId).isEqualTo(userId)
        assertThat(user.name).isEqualTo(name)
        assertThat(user.email).isEqualTo(email)
        assertThat(user).extracting("encryptedPassword").isNotEqualTo(password)
    }

    @Test
    fun `createUser() should encrypt password when creating user`() {
        // Arrange
        val userId = "testId"
        val password = "testPassword"
        val name = "testName"
        val birthDate = LocalDate.now()
        val email = "test@email.com"

        // Act
        val user = userService.createUser(userId, password, name, birthDate, email)

        // Assert
        assertThat(user.encryptedPassword).isNotEqualTo(password)
        assertThat(user.encryptedPassword).startsWith("$2a$") // BCrypt format
        assertThat(passwordEncoder.matches(password, user.encryptedPassword)).isTrue()
    }

    @Test
    fun `getUserByUserId() returns User`() {
        // Arrange
        val userId = "testId"
        val password = "testPassword"
        userService.createUser(userId, password, "testName", LocalDate.now(), "test@email.com")

        // Act
        val userFound = userService.getUserByUserId(userId)
        
        // Assert
        assertThat(userFound).isNotNull
        assertThat(userFound.userId).isEqualTo(userId)
    }

    @Test
    fun `authenticate() returns User with correct password`() {
        // Arrange
        val userId = "testId"
        val password = "testPassword"
        userService.createUser(userId, password, "testName", LocalDate.now(), "test@email.com")

        // Act
        val authenticatedUser = userService.authenticate(userId, password)

        // Assert
        assertThat(authenticatedUser).isNotNull
        assertThat(authenticatedUser.userId).isEqualTo(userId)
    }

    @Test
    fun `changePassword() should change password and authenticate with new password`() {
        // Arrange
        val userId = "testId"
        val oldPassword = "testPassword"
        val newPassword = "newPassword123!"
        userService.createUser(userId, oldPassword, "testName", LocalDate.now(), "test@email.com")

        // Act
        userService.changePassword(userId, oldPassword, newPassword)

        // Assert - 새 비밀번호로 인증 가능한지 확인
        val authenticatedUser = userService.authenticate(userId, newPassword)
        assertThat(authenticatedUser.userId).isEqualTo(userId)
    }
}
