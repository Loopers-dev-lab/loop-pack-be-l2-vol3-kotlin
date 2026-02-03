package com.loopers.domain.user

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
                loginId = "testuser1", password = "Password1!",
                name = "홍길동", birthDate = LocalDate.of(1990, 1, 15),
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
}
