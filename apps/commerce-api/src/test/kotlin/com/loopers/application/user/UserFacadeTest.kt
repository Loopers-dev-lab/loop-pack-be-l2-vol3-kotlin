package com.loopers.application.user

import com.loopers.domain.BaseEntity
import com.loopers.domain.point.FakePointHistoryRepository
import com.loopers.domain.point.FakeUserPointRepository
import com.loopers.domain.point.UserPointService
import com.loopers.domain.user.UserCommand
import com.loopers.domain.user.UserService
import com.loopers.domain.user.entity.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UserFacadeTest {

    private lateinit var userService: UserService
    private lateinit var userPointRepository: FakeUserPointRepository
    private lateinit var pointHistoryRepository: FakePointHistoryRepository
    private lateinit var userPointService: UserPointService
    private lateinit var userFacade: UserFacade

    @BeforeEach
    fun setUp() {
        userService = mockk()
        userPointRepository = FakeUserPointRepository()
        pointHistoryRepository = FakePointHistoryRepository()
        userPointService = UserPointService(userPointRepository, pointHistoryRepository)
        userFacade = UserFacade(userService, userPointService)
    }

    @Nested
    @DisplayName("signUp 시")
    inner class SignUp {

        @Test
        @DisplayName("User 생성과 함께 초기 잔액 0의 UserPoint가 생성된다")
        fun signUp_createsUserAndUserPoint() {
            // arrange
            val command = UserCommand.SignUp(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            slot<User>()
            every { userService.signUp(command) } answers {
                val user = User(
                    loginId = command.loginId,
                    password = command.password,
                    name = command.name,
                    birthDate = command.birthDate,
                    email = command.email,
                )
                // Fake ID 설정
                BaseEntity::class.java.getDeclaredField("id").apply {
                    isAccessible = true
                    set(user, 1L)
                }
                user
            }

            // act
            val result = userFacade.signUp(command)

            // assert
            assertThat(result.loginId).isEqualTo("testuser1")

            val userPoint = userPointRepository.findByUserId(1L)
            assertThat(userPoint).isNotNull
            assertThat(userPoint!!.balance).isEqualTo(0)
        }
    }
}
