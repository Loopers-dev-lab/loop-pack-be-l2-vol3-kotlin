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
    fun `createUser() throws CoreException when userId already exists`() {
        // Arrange
        val userId = "duplicateUser"
        userService.createUser(userId, "password123!", "User1", LocalDate.now(),
            "duplicateUser@example.com")

        // Act
        val exception = assertThrows<CoreException> {
            userService.createUser(userId, "password123!", "User2", LocalDate.now(),
                "duplicateUser@example.com")
        }

        // Assert
        assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
    }

    @Test
    fun `createUser() throws CoreException when password is too short`() {
        // Arrange
        val shortPassword = "pass123" // 7 chars (< 8)

        // Act
        val exception = assertThrows<CoreException> {
            userService.createUser("testUser", shortPassword, "홍길동", LocalDate.now(),
                "test@example.com")
        }

        // Assert
        assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        assertThat(exception.message).contains("8~16자")
    }

    @Test
    fun `createUser() throws CoreException when password is too long`() {
        // Arrange
        val shortPassword = "password123456789!" // 18 chars (> 16)

        // Act
        val exception = assertThrows<CoreException> {
            userService.createUser("testUser", shortPassword, "홍길동", LocalDate.now(),
                "test@example.com")
        }

        // Assert
        assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        assertThat(exception.message).contains("8~16자")
    }

    @Test
    fun `createUser() accept with exact 8 characters`() {
        // Arrange
        val shortPassword = "pass1234" // 8 chars

        // Act
        val user = userService.createUser(
            "testUser", shortPassword, "홍길동", LocalDate.now(),
            "test@example.com"
        )

        // Assert
        assertThat(user).isNotNull
    }

    @Test
    fun `createUser() accept with exact 16 characters`() {
        // Arrange                                                                         
        val password = "password12345678" // 16 chars                                      

        // Act                                                                             
        val user = userService.createUser(
            "testUser", password, "testName", LocalDate.now(),
            "test@example.com"
        )

        // Assert                                                                          
        assertThat(user).isNotNull
    }

    @Test
    fun `createUser() throws CoreException when password contains birthDate in yyyyMMdd format`() {
        // Arrange                                                                         
        val birthDate = LocalDate.of(1990, 1, 1)
        val password = "pass19900101" // contains 19900101                                 

        // Act                                                                             
        val exception = assertThrows<CoreException> {
            userService.createUser("testUser", password, "testName", birthDate,
                "test@example.com")
        }

        // Assert                                                                          
        assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        assertThat(exception.message).contains("생년월일")
    }

    @Test
    fun `createUser() throws CoreException when password contains birthDate in yyMMdd format`() {
        // Arrange                                                                         
        val birthDate = LocalDate.of(1990, 1, 1)
        val password = "pass900101abc" // contains 900101                                  

        // Act                                                                             
        val exception = assertThrows<CoreException> {
            userService.createUser("testUser", password, "testName", birthDate,
                "test@example.com")
        }

        // Assert                                                                          
        assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        assertThat(exception.message).contains("생년월일")
    }

    @Test
    fun `createUser() throws CoreException when password contains birthDate in MMdd format`() {
        // Arrange                                                                         
        val birthDate = LocalDate.of(1990, 1, 1)
        val password = "password0101" // contains 0101                                     

        // Act                                                                             
        val exception = assertThrows<CoreException> {
            userService.createUser("testUser", password, "testName", birthDate,
                "test@example.com")
        }

        // Assert                                                                          
        assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        assertThat(exception.message).contains("생년월일")
    }

    @Test
    fun `createUser() should accept password that does not contain birthDate`() {
        // Arrange                                                                         
        val birthDate = LocalDate.of(1990, 1, 1)
        val password = "safePassword!" // does not contain any birthDate format            

        // Act                                                                             
        val user = userService.createUser("testUser", password, "testName", birthDate,
            "test@example.com")

        // Assert                                                                          
        assertThat(user).isNotNull
    }

    @Test
    fun `createUser() throws CoreException when email format is invalid`() {
        // Arrange                                                                         
        val invalidEmail = "invalid-email-format" // no @                                  

        // Act                                                                             
        val exception = assertThrows<CoreException> {
            userService.createUser("testUser", "password123!", "testName", LocalDate.now(),
                invalidEmail)
        }

        // Assert                                                                          
        assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        assertThat(exception.message).contains("이메일")
    }

    @Test
    fun `createUser() should accept valid email format`() {
        // Arrange                                                                         
        val validEmail = "valid.email@example.com"

        // Act                                                                             
        val user = userService.createUser("testUser", "password123!", "testName",
            LocalDate.now(), validEmail)

        // Assert                                                                          
        assertThat(user.email).isEqualTo(validEmail)
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
    fun `getUserByUserId() throws CoreException when User is not found`() {
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
    fun `authenticate() throws exception with wrong password`() {
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
    fun `authenticate() throws exception with non-existing User`() {
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
