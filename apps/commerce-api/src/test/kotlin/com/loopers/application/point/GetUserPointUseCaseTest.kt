package com.loopers.application.point

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.point.FakeUserPointRepository
import com.loopers.domain.point.model.UserPoint
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GetUserPointUseCaseTest {

    private lateinit var userPointRepository: FakeUserPointRepository
    private lateinit var getUserPointUseCase: GetUserPointUseCase

    @BeforeEach
    fun setUp() {
        userPointRepository = FakeUserPointRepository()
        getUserPointUseCase = GetUserPointUseCase(userPointRepository)
    }

    @Nested
    @DisplayName("execute 시")
    inner class Execute {

        @Test
        @DisplayName("존재하는 사용자의 포인트를 조회하면 PointBalanceInfo가 반환된다")
        fun execute_existingUser_returnsPointBalanceInfo() {
            // arrange
            userPointRepository.save(UserPoint(refUserId = UserId(1)))

            // act
            val result = getUserPointUseCase.execute(1L)

            // assert
            assertThat(result.balance).isEqualTo(0)
        }

        @Test
        @DisplayName("존재하지 않는 사용자를 조회하면 NOT_FOUND 예외가 발생한다")
        fun execute_nonExistentUser_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                getUserPointUseCase.execute(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
