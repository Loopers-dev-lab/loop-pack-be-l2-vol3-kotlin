package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate


class UserModelTest {

    @Nested
    @DisplayName("UserModel 초기화 시")
    inner class Initialize {
        @Test
        @DisplayName("ID/PW/이름/생년월일/이메일로 UserModel 을 정상적으로 생성한다.")
        fun initializeUserModel_whenIdPasswordNameBirthDateEmail() {
            // Arrange
            val userId = "testId"
            val password = "testPassword"
            val name = "testUserName"
            val birthDate = LocalDate.now()
            val email = "testEmail@example.com"

            // Act
            val userModel: UserModel = UserModel(
                userId = userId,
                password = password,
                name = name,
                birthDate = birthDate,
                email = email
            )

            // Assert
            assertEquals(userId, userModel.userId)
            assertEquals(name, userModel.name)
            assertEquals(email, userModel.email)
        }
        
        @Test
        fun throwsBadRequestException_whenPrameterIsBlank() {
            // Arrange
            val userId = " "
            val password = "testPassword"
            val name = "testUserName"
            val birthDate = LocalDate.now()
            val email = "testEmail@example.com"

            // Act
            val noId = assertThrows<CoreException> {
                UserModel(
                    userId = userId,
                    password = password,
                    name = name,
                    birthDate = birthDate,
                    email = email
                )
            }
            
            // Assert
            assertThat(noId.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
