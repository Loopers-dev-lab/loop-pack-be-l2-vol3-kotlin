package com.loopers.application.queue

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.token.FakeEntryTokenRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class ValidateEntryTokenUseCaseTest {

    private lateinit var entryTokenRepository: FakeEntryTokenRepository
    private lateinit var validateEntryTokenUseCase: ValidateEntryTokenUseCase

    @BeforeEach
    fun setUp() {
        entryTokenRepository = FakeEntryTokenRepository()
        validateEntryTokenUseCase = ValidateEntryTokenUseCase(entryTokenRepository)
    }

    @Nested
    @DisplayName("execute 시")
    inner class Execute {

        @Test
        @DisplayName("토큰이 만료되어 Redis에 없으면 FORBIDDEN 예외가 발생한다")
        fun execute_tokenExpired_throwsForbidden() {
            // act
            val exception = assertThrows<CoreException> {
                validateEntryTokenUseCase.execute(1L, "expired-token")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.FORBIDDEN)
        }

        @Test
        @DisplayName("저장된 토큰과 불일치하면 FORBIDDEN 예외가 발생한다")
        fun execute_tokenMismatch_throwsForbidden() {
            // arrange
            entryTokenRepository.issue(UserId(1L), "correct-token", 300)

            // act
            val exception = assertThrows<CoreException> {
                validateEntryTokenUseCase.execute(1L, "wrong-token")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.FORBIDDEN)
        }

        @Test
        @DisplayName("유효한 토큰이면 정상 통과한다")
        fun execute_validToken_passes() {
            // arrange
            entryTokenRepository.issue(UserId(1L), "valid-token", 300)

            // act & assert
            assertDoesNotThrow {
                validateEntryTokenUseCase.execute(1L, "valid-token")
            }
        }

        @Test
        @DisplayName("검증 성공 후 토큰이 즉시 삭제(소비)된다")
        fun execute_validToken_deletesToken() {
            // arrange
            entryTokenRepository.issue(UserId(1L), "valid-token", 300)

            // act
            validateEntryTokenUseCase.execute(1L, "valid-token")

            // assert
            assertThat(entryTokenRepository.find(UserId(1L))).isNull()
        }
    }
}
