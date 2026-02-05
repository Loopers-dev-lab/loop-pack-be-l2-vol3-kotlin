package com.loopers.domain.member

import com.loopers.infrastructure.member.MemberJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest
class MemberServiceIntegrationTest @Autowired constructor(
    private val memberService: MemberService,
    private val memberJpaRepository: MemberJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    @DisplayName("회원가입")
    inner class Register {
        @Test
        @DisplayName("유효한 정보로 회원가입에 성공한다")
        fun registerSuccess() {
            // Arrange
            val command = RegisterCommand(
                loginId = "testuser",
                password = "password123",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "test@example.com",
            )

            // Act
            val result = memberService.register(command)

            // Assert
            assertThat(result).isNotNull
            assertThat(result.loginId).isEqualTo("testuser")
            assertThat(result.name).isEqualTo("홍길동")
            assertThat(result.email).isEqualTo("test@example.com")
            assertThat(result.birthDate).isEqualTo(LocalDate.of(1990, 1, 1))

            val savedMember = memberJpaRepository.findAll().first()
            assertThat(savedMember.loginId).isEqualTo("testuser")
        }

        @Test
        @DisplayName("이미 존재하는 loginId로 가입 시 CONFLICT 예외가 발생한다")
        fun registerDuplicateLoginId() {
            // Arrange
            val command = RegisterCommand(
                loginId = "duplicate",
                password = "password123",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "first@example.com",
            )
            memberService.register(command)

            val duplicateCommand = RegisterCommand(
                loginId = "duplicate",
                password = "password456",
                name = "김철수",
                birthDate = LocalDate.of(1992, 2, 2),
                email = "second@example.com",
            )

            // Act & Assert
            val exception = assertThrows<CoreException> {
                memberService.register(duplicateCommand)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @Test
        @DisplayName("비밀번호가 암호화되어 저장된다")
        fun passwordEncrypted() {
            // Arrange
            val rawPassword = "password123"
            val command = RegisterCommand(
                loginId = "testuser",
                password = rawPassword,
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "test@example.com",
            )

            // Act
            memberService.register(command)

            // Assert
            val savedMember = memberJpaRepository.findAll().first()
            assertThat(savedMember.password).isNotEqualTo(rawPassword)
            assertThat(savedMember.password).isNotBlank()
        }
    }

    @Nested
    @DisplayName("내 정보 조회")
    inner class GetMyInfo {
        @Test
        @DisplayName("유효한 loginId와 password로 조회에 성공한다")
        fun getMyInfoSuccess() {
            // Arrange
            val command = RegisterCommand(
                loginId = "testuser",
                password = "password123",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "test@example.com",
            )
            memberService.register(command)

            // Act
            val result = memberService.getMyInfo("testuser", "password123")

            // Assert
            assertThat(result).isNotNull
            assertThat(result.loginId).isEqualTo("testuser")
            assertThat(result.name).isEqualTo("홍길동")
            assertThat(result.email).isEqualTo("test@example.com")
            assertThat(result.birthDate).isEqualTo(LocalDate.of(1990, 1, 1))
        }

        @Test
        @DisplayName("존재하지 않는 loginId로 조회 시 NOT_FOUND 예외가 발생한다")
        fun getMyInfoNotFound() {
            // Arrange - 아무 데이터도 없는 상태

            // Act & Assert
            val exception = assertThrows<CoreException> {
                memberService.getMyInfo("nonexistent", "password123")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("잘못된 password로 조회 시 BAD_REQUEST 예외가 발생한다")
        fun getMyInfoWrongPassword() {
            // Arrange
            val command = RegisterCommand(
                loginId = "testuser",
                password = "correctpassword",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "test@example.com",
            )
            memberService.register(command)

            // Act & Assert
            val exception = assertThrows<CoreException> {
                memberService.getMyInfo("testuser", "wrongpassword")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Nested
    @DisplayName("비밀번호 수정")
    inner class ChangePassword {
        @Test
        @DisplayName("유효한 기존 비밀번호와 새 비밀번호로 변경에 성공한다")
        fun changePasswordSuccess() {
            // Arrange
            val command = RegisterCommand(
                loginId = "testuser",
                password = "oldpassword",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "test@example.com",
            )
            memberService.register(command)

            // Act
            memberService.changePassword("testuser", "oldpassword", "newpassword")

            // Assert
            val result = memberService.getMyInfo("testuser", "newpassword")
            assertThat(result).isNotNull
            assertThat(result.loginId).isEqualTo("testuser")
        }

        @Test
        @DisplayName("기존 비밀번호가 틀리면 BAD_REQUEST 예외가 발생한다")
        fun changePasswordWrongCurrentPassword() {
            // Arrange
            val command = RegisterCommand(
                loginId = "testuser",
                password = "oldpassword",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "test@example.com",
            )
            memberService.register(command)

            // Act & Assert
            val exception = assertThrows<CoreException> {
                memberService.changePassword("testuser", "wrongpassword", "newpassword")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("새 비밀번호가 현재 비밀번호와 동일하면 BAD_REQUEST 예외가 발생한다")
        fun changePasswordSameAsCurrentPassword() {
            // Arrange
            val command = RegisterCommand(
                loginId = "testuser",
                password = "samepassword",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "test@example.com",
            )
            memberService.register(command)

            // Act & Assert
            val exception = assertThrows<CoreException> {
                memberService.changePassword("testuser", "samepassword", "samepassword")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
