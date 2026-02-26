package com.loopers.application.user

import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.UserErrorCode
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
class GetMyInfoUseCaseTest @Autowired constructor(
    private val getMyInfoUseCase: GetMyInfoUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("내 정보 조회")
    @Nested
    inner class Execute {

        @DisplayName("존재하는 사용자의 마스킹된 정보를 반환한다")
        @Test
        fun success() {
            registerUserUseCase.execute(
                UserCommand.Register(
                    loginId = "testuser",
                    rawPassword = "Test123!",
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 1),
                    email = "test@example.com",
                ),
            )
            val userId = userJpaRepository.findByLoginId("testuser")!!.id!!

            val result = getMyInfoUseCase.execute(userId)

            assertThat(result.name).isEqualTo("홍길*")
        }

        @DisplayName("존재하지 않는 사용자 ID이면 AUTHENTICATION_FAILED 예외가 발생한다")
        @Test
        fun failWhenUserNotFound() {
            val exception = assertThrows<CoreException> {
                getMyInfoUseCase.execute(999L)
            }
            assertThat(exception.errorCode).isEqualTo(UserErrorCode.AUTHENTICATION_FAILED)
        }
    }
}
