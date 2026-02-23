package com.loopers.application.user

import com.loopers.domain.point.FakePointHistoryRepository
import com.loopers.domain.point.FakeUserPointRepository
import com.loopers.domain.point.UserPointService
import com.loopers.domain.user.FakeUserRepository
import com.loopers.domain.user.UserCommand
import com.loopers.domain.user.UserService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RegisterUserUseCaseTest {

    private lateinit var userService: UserService
    private lateinit var userPointRepository: FakeUserPointRepository
    private lateinit var pointHistoryRepository: FakePointHistoryRepository
    private lateinit var userPointService: UserPointService
    private lateinit var registerUserUseCase: RegisterUserUseCase

    @BeforeEach
    fun setUp() {
        val userRepository = FakeUserRepository()
        userService = UserService(userRepository)
        userPointRepository = FakeUserPointRepository()
        pointHistoryRepository = FakePointHistoryRepository()
        userPointService = UserPointService(userPointRepository, pointHistoryRepository)
        registerUserUseCase = RegisterUserUseCase(userService, userPointService)
    }

    @Nested
    @DisplayName("execute 시")
    inner class Execute {

        @Test
        @DisplayName("User 생성과 함께 초기 잔액 0의 UserPoint가 생성된다")
        fun execute_createsUserAndUserPoint() {
            // arrange
            val command = UserCommand.SignUp(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            // act
            val result = registerUserUseCase.execute(
                loginId = command.loginId,
                password = command.password,
                name = command.name,
                birthDate = command.birthDate,
                email = command.email,
            )

            // assert
            assertThat(result.loginId).isEqualTo("testuser1")

            val userPoint = userPointRepository.findByUserId(result.id)
            assertThat(userPoint).isNotNull
            assertThat(userPoint!!.balance).isEqualTo(0)
        }
    }
}
