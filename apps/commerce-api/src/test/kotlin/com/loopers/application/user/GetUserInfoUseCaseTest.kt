package com.loopers.application.user

import com.loopers.domain.user.FakeUserRepository
import com.loopers.domain.user.UserTestFixture
import com.loopers.domain.user.model.User
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime

class GetUserInfoUseCaseTest {

    private lateinit var userRepository: FakeUserRepository
    private lateinit var getUserInfoUseCase: GetUserInfoUseCase

    @BeforeEach
    fun setUp() {
        userRepository = FakeUserRepository()
        getUserInfoUseCase = GetUserInfoUseCase(userRepository)
    }

    @Nested
    @DisplayName("execute 시")
    inner class Execute {

        @Test
        @DisplayName("존재하는 사용자를 조회하면 UserInfo를 반환한다")
        fun execute_userExists_returnsUserInfo() {
            // arrange
            val user = userRepository.save(UserTestFixture.createUser())

            // act
            val result = getUserInfoUseCase.execute(user.id)

            // assert
            assertThat(result.loginId).isEqualTo(UserTestFixture.DEFAULT_LOGIN_ID)
            assertThat(result.name).isEqualTo(UserTestFixture.DEFAULT_NAME)
        }

        @Test
        @DisplayName("삭제된 사용자를 조회하면 NOT_FOUND 예외가 발생한다")
        fun execute_deletedUser_throwsNotFound() {
            // arrange
            val user = UserTestFixture.createUser()
            val deletedUser = User(
                loginId = user.loginId,
                password = user.password,
                name = user.name,
                birthDate = user.birthDate,
                email = user.email,
                deletedAt = ZonedDateTime.now(),
            )
            val saved = userRepository.save(deletedUser)

            // act
            val exception = assertThrows<CoreException> {
                getUserInfoUseCase.execute(saved.id)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("존재하지 않는 사용자이면 NOT_FOUND 예외가 발생한다")
        fun execute_userNotFound_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                getUserInfoUseCase.execute(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
            assertThat(exception.message).isEqualTo("사용자를 찾을 수 없습니다.")
        }
    }
}
