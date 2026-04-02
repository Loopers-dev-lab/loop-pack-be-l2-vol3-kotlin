package com.loopers.domain.queue

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("WaitingQueueService")
class WaitingQueueServiceTest {

    private val waitingQueueRepository: WaitingQueueRepository = mockk()
    private val entryTokenRepository: EntryTokenRepository = mockk()
    private val waitingQueueService = WaitingQueueService(waitingQueueRepository, entryTokenRepository)

    companion object {
        private const val USER_ID = 1L
    }

    @DisplayName("enterQueue")
    @Nested
    inner class EnterQueue {
        @DisplayName("대기열에 정상적으로 진입하면 순번과 전체 대기 인원을 반환한다")
        @Test
        fun entersQueue_whenNewUser() {
            // arrange
            every { entryTokenRepository.hasToken(USER_ID) } returns false
            every { waitingQueueRepository.enter(USER_ID, any()) } returns true
            every { waitingQueueRepository.getPosition(USER_ID) } returns 0L
            every { waitingQueueRepository.getTotalWaitingCount() } returns 1L

            // act
            val result = waitingQueueService.enterQueue(USER_ID)

            // assert
            assertThat(result.position).isEqualTo(1)
            assertThat(result.totalWaiting).isEqualTo(1)
            verify { waitingQueueRepository.enter(USER_ID, any()) }
        }

        @DisplayName("이미 대기열에 존재하는 사용자가 재진입하면 CONFLICT 예외가 발생한다")
        @Test
        fun throwsConflict_whenUserAlreadyInQueue() {
            // arrange
            every { entryTokenRepository.hasToken(USER_ID) } returns false
            every { waitingQueueRepository.enter(USER_ID, any()) } returns false

            // act & assert
            assertThatThrownBy { waitingQueueService.enterQueue(USER_ID) }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.CONFLICT)
        }

        @DisplayName("이미 입장 토큰이 발급된 사용자가 대기열에 진입하면 CONFLICT 예외가 발생한다")
        @Test
        fun throwsConflict_whenUserAlreadyHasToken() {
            // arrange
            every { entryTokenRepository.hasToken(USER_ID) } returns true

            // act & assert
            assertThatThrownBy { waitingQueueService.enterQueue(USER_ID) }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.CONFLICT)
        }
    }

    @DisplayName("getQueuePosition")
    @Nested
    inner class GetQueuePosition {
        @DisplayName("대기 중인 사용자의 순번과 예상 대기 시간을 반환한다")
        @Test
        fun returnsPosition_whenUserIsWaiting() {
            // arrange
            every { entryTokenRepository.getToken(USER_ID) } returns null
            every { waitingQueueRepository.getPosition(USER_ID) } returns 99L
            every { waitingQueueRepository.getTotalWaitingCount() } returns 200L

            // act
            val result = waitingQueueService.getQueuePosition(USER_ID)

            // assert
            assertThat(result.position).isEqualTo(100)
            assertThat(result.totalWaiting).isEqualTo(200)
            assertThat(result.token).isNull()
        }

        @DisplayName("토큰이 발급된 사용자는 position 0과 토큰을 반환한다")
        @Test
        fun returnsTokenAndPositionZero_whenTokenIssued() {
            // arrange
            val token = "test-token-uuid"
            every { entryTokenRepository.getToken(USER_ID) } returns token
            every { waitingQueueRepository.getTotalWaitingCount() } returns 50L

            // act
            val result = waitingQueueService.getQueuePosition(USER_ID)

            // assert
            assertThat(result.position).isEqualTo(0)
            assertThat(result.token).isEqualTo(token)
        }

        @DisplayName("대기열에 없는 사용자를 조회하면 NOT_FOUND 예외가 발생한다")
        @Test
        fun throwsNotFound_whenUserNotInQueue() {
            // arrange
            every { entryTokenRepository.getToken(USER_ID) } returns null
            every { waitingQueueRepository.getPosition(USER_ID) } returns null

            // act & assert
            assertThatThrownBy { waitingQueueService.getQueuePosition(USER_ID) }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("processQueue")
    @Nested
    inner class ProcessQueue {
        @DisplayName("대기열에서 N명을 꺼내 입장 토큰을 발급한다")
        @Test
        fun issuesTokens_whenUsersAreWaiting() {
            // arrange
            val userIds = setOf("1", "2", "3")
            every { waitingQueueRepository.popMinN(any()) } returns userIds
            every { entryTokenRepository.issueToken(any(), any(), any()) } returns Unit

            // act
            val result = waitingQueueService.processQueue()

            // assert
            assertThat(result).isEqualTo(3)
            verify(exactly = 3) { entryTokenRepository.issueToken(any(), any(), WaitingQueueService.TOKEN_TTL_SECONDS) }
        }

        @DisplayName("대기열이 비어있으면 0을 반환한다")
        @Test
        fun returnsZero_whenQueueIsEmpty() {
            // arrange
            every { waitingQueueRepository.popMinN(any()) } returns emptySet()

            // act
            val result = waitingQueueService.processQueue()

            // assert
            assertThat(result).isEqualTo(0)
            verify(exactly = 0) { entryTokenRepository.issueToken(any(), any(), any()) }
        }
    }

    @DisplayName("validateEntryToken")
    @Nested
    inner class ValidateEntryToken {
        @DisplayName("유효한 토큰이 있으면 토큰을 반환한다")
        @Test
        fun returnsToken_whenValid() {
            // arrange
            val token = "valid-token"
            every { entryTokenRepository.getToken(USER_ID) } returns token

            // act
            val result = waitingQueueService.validateEntryToken(USER_ID)

            // assert
            assertThat(result).isEqualTo(token)
        }

        @DisplayName("토큰이 없으면 UNAUTHORIZED 예외가 발생한다")
        @Test
        fun throwsUnauthorized_whenNoToken() {
            // arrange
            every { entryTokenRepository.getToken(USER_ID) } returns null

            // act & assert
            assertThatThrownBy { waitingQueueService.validateEntryToken(USER_ID) }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.UNAUTHORIZED)
        }
    }
}
