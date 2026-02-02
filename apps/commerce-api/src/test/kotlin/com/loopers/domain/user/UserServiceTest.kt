package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate


@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest @Autowired constructor(
    private val userService: UserService,
    private val databaseCleanUp: DatabaseCleanUp
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    fun createUser_shouldCreateUserWithEncryptedPassword() {
        // Arrange
        val userId = "testId"
        val password = "testPassword"
        val name = "testName"
        val birthDate = LocalDate.now()
        val email = "test@email.com"

        // Act
        val user = userService.createUser(userId, password, name, birthDate, email)

        // Assert
        assertThat(user.id).isEqualTo(userId)
        assertThat(user.name).isEqualTo(name)
        assertThat(user.email).isEqualTo(email)
        assertThat(user).extracting("encryptedPassword").isNotEqualTo(password)
    }

    @Test
    fun createUser_throwsExceptionWhenEmailAlreadyExists() {
        // Arrange
        val userId = "testId"
        val password = "testPassword"
        val name = "testName"
        val birthDate = LocalDate.now()
        val email = "duplicate@email.com"
        userService.createUser(userId, password, name, birthDate, email)

        // Act

        // Assert
        assertThrows<CoreException> {
            userService.createUser(userId, "newPassword", name, birthDate, email)
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.CONFLICT)
        }
    }
    
    @Test
    fun getUserByUserId_returnsUser() {
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
    fun getUserByUserId_throwsExceptionWhenUserNotFound() {
        // Arrange

        // Act

        // Assert
        assertThrows<CoreException> {
            userService.getUserByUserId("nonExistentUser")
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Test
    fun authenticate_returnsUserWithCorrectPassword() {
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
    fun authenticate_throwsExceptionWithWrongPassword() {
        // Arrange
        val userId = "testId"
        val password = "testPassword"
        userService.createUser(userId, password, "testName", LocalDate.now(), "test@email.com")

        // Act

        // Assert
        assertThrows<CoreException> {
            userService.authenticate(userId, "wrongPassword")
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }
    }

    @Test
    fun authenticate_throwsExceptionWithNonExistingUser() {
        // Arrange

        // Act

        // Assert
        assertThrows<CoreException> {
            userService.authenticate("nonExistUser", "wrongPassword")
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }
    }
}
