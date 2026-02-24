package com.loopers.application.user

import com.loopers.domain.point.FakeUserPointRepository
import com.loopers.domain.user.FakeUserRepository
import com.loopers.domain.user.UserTestFixture
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class RegisterUserUseCaseTest {

    private lateinit var userRepository: FakeUserRepository
    private lateinit var userPointRepository: FakeUserPointRepository
    private lateinit var registerUserUseCase: RegisterUserUseCase

    @BeforeEach
    fun setUp() {
        userRepository = FakeUserRepository()
        userPointRepository = FakeUserPointRepository()
        registerUserUseCase = RegisterUserUseCase(userRepository, userPointRepository)
    }

    @Nested
    @DisplayName("execute 시")
    inner class Execute {

        @Test
        @DisplayName("유효한 정보로 가입하면 User 생성과 함께 초기 잔액 0의 UserPoint가 생성된다")
        fun execute_createsUserAndUserPoint() {
            // act
            val result = registerUserUseCase.execute(
                loginId = UserTestFixture.DEFAULT_LOGIN_ID,
                password = UserTestFixture.DEFAULT_PASSWORD,
                name = UserTestFixture.DEFAULT_NAME,
                birthDate = UserTestFixture.DEFAULT_BIRTH_DATE,
                email = UserTestFixture.DEFAULT_EMAIL,
            )

            // assert
            assertThat(result.loginId).isEqualTo(UserTestFixture.DEFAULT_LOGIN_ID)

            val userPoint = userPointRepository.findByUserId(result.id)
            assertThat(userPoint).isNotNull
            assertThat(userPoint!!.balance).isEqualTo(0)
        }

        @Test
        @DisplayName("이미 존재하는 로그인 ID로 가입하면 CONFLICT 예외가 발생한다")
        fun execute_duplicateLoginId_throwsConflict() {
            // arrange
            registerUserUseCase.execute(
                loginId = UserTestFixture.DEFAULT_LOGIN_ID,
                password = UserTestFixture.DEFAULT_PASSWORD,
                name = UserTestFixture.DEFAULT_NAME,
                birthDate = UserTestFixture.DEFAULT_BIRTH_DATE,
                email = UserTestFixture.DEFAULT_EMAIL,
            )

            // act
            val exception = assertThrows<CoreException> {
                registerUserUseCase.execute(
                    loginId = UserTestFixture.DEFAULT_LOGIN_ID,
                    password = "Password2!",
                    name = "김철수",
                    birthDate = LocalDate.of(1995, 5, 20),
                    email = "other@example.com",
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
            assertThat(exception.message).isEqualTo("이미 존재하는 로그인 ID입니다.")
        }
    }
}
