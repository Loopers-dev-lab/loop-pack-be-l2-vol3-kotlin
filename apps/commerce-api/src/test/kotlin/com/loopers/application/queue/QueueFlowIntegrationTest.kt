package com.loopers.application.queue

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.token.FakeEntryTokenRepository
import com.loopers.domain.queue.waiting.FakeWaitingQueueRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("대기열 전체 흐름 통합 테스트 (Fake 기반)")
class QueueFlowIntegrationTest {

    private lateinit var waitingQueueRepository: FakeWaitingQueueRepository
    private lateinit var entryTokenRepository: FakeEntryTokenRepository
    private lateinit var enterQueueUseCase: EnterQueueUseCase
    private lateinit var getQueuePositionUseCase: GetQueuePositionUseCase
    private lateinit var issueEntryTokensUseCase: IssueEntryTokensUseCase
    private lateinit var validateEntryTokenUseCase: ValidateEntryTokenUseCase

    private val defaultProperties = QueueProperties(
        maxCapacity = 100,
        batchSize = 5,
        tokenTtlSeconds = 300,
        throughputTps = 175,
        schedulerDelayMs = 100,
        jitterMaxMs = 0,
    )

    @BeforeEach
    fun setUp() {
        entryTokenRepository = FakeEntryTokenRepository()
        waitingQueueRepository = FakeWaitingQueueRepository(entryTokenRepository)
        enterQueueUseCase = EnterQueueUseCase(waitingQueueRepository, entryTokenRepository, defaultProperties)
        getQueuePositionUseCase = GetQueuePositionUseCase(waitingQueueRepository, entryTokenRepository, defaultProperties)
        issueEntryTokensUseCase = IssueEntryTokensUseCase(waitingQueueRepository, defaultProperties)
        validateEntryTokenUseCase = ValidateEntryTokenUseCase(entryTokenRepository)
    }

    @Nested
    @DisplayName("동시 진입")
    inner class ConcurrentEntry {

        @Test
        @DisplayName("100명 진입 시 순번이 중복 없이 보장된다")
        fun hundredUsersEnter_positionsAreUnique() {
            // arrange & act
            val positions = (1L..100L).map { userId ->
                enterQueueUseCase.execute(userId).position
            }

            // assert
            assertThat(positions).hasSize(100)
            assertThat(positions.toSet()).hasSize(100)
        }
    }

    @Nested
    @DisplayName("토큰 만료")
    inner class TokenExpiration {

        @Test
        @DisplayName("발급된 토큰이 만료된 후 검증 시 FORBIDDEN 예외가 발생한다")
        fun expiredToken_throwsForbidden() {
            // arrange — 진입 → 토큰 발급
            enterQueueUseCase.execute(1L)
            issueEntryTokensUseCase.execute()
            val token = entryTokenRepository.find(UserId(1L))!!

            // act — 토큰 만료 시뮬레이션
            entryTokenRepository.delete(UserId(1L))

            // assert
            assertThatThrownBy { validateEntryTokenUseCase.execute(1L, token) }
                .isInstanceOf(CoreException::class.java)
                .extracting("errorType")
                .isEqualTo(ErrorType.FORBIDDEN)
        }
    }

    @Nested
    @DisplayName("대기열 상한 초과")
    inner class QueueCapacityExceeded {

        @Test
        @DisplayName("상한 초과 진입 시 TOO_MANY_REQUESTS 예외가 발생한다")
        fun exceedCapacity_throwsTooManyRequests() {
            // arrange — maxCapacity = 10으로 제한
            val limitedEnterUseCase = EnterQueueUseCase(
                waitingQueueRepository,
                entryTokenRepository,
                defaultProperties.copy(maxCapacity = 10),
            )
            (1L..10L).forEach { limitedEnterUseCase.execute(it) }

            // act & assert — 11번째 진입 시 예외
            assertThatThrownBy { limitedEnterUseCase.execute(11L) }
                .isInstanceOf(CoreException::class.java)
                .extracting("errorType")
                .isEqualTo(ErrorType.TOO_MANY_REQUESTS)
        }
    }

    @Nested
    @DisplayName("토큰 없이 주문 시도")
    inner class OrderWithoutToken {

        @Test
        @DisplayName("토큰 없이 검증 시 FORBIDDEN 예외가 발생한다")
        fun noToken_throwsForbidden() {
            // act & assert
            assertThatThrownBy { validateEntryTokenUseCase.execute(1L, "invalid-token") }
                .isInstanceOf(CoreException::class.java)
                .extracting("errorType")
                .isEqualTo(ErrorType.FORBIDDEN)
        }
    }

    @Nested
    @DisplayName("정상 플로우 E2E")
    inner class NormalFlowE2E {

        @Test
        @DisplayName("진입 → 스케줄러 실행 → 토큰 발급 → 검증 성공 → 토큰 소비")
        fun fullFlow_enterToTokenConsumption() {
            // act 1 — 대기열 진입
            val enterResult = enterQueueUseCase.execute(1L)
            assertThat(enterResult.position).isZero()
            assertThat(enterResult.token).isNull()

            // act 2 — 순번 조회
            val positionResult = getQueuePositionUseCase.execute(1L)
            assertThat(positionResult.position).isZero()

            // act 3 — 스케줄러 실행 → 토큰 발급
            issueEntryTokensUseCase.execute()
            val token = entryTokenRepository.find(UserId(1L))!!

            // act 4 — 토큰 보유 상태에서 대기열 재진입 시 토큰 반환
            val reEnterResult = enterQueueUseCase.execute(1L)
            assertThat(reEnterResult.token).isEqualTo(token)

            // act 5 — 토큰 검증 성공
            validateEntryTokenUseCase.execute(1L, token)

            // act 6 — 주문 완료 후 토큰 소비 (PlaceOrderUseCase 동작 시뮬레이션)
            entryTokenRepository.delete(UserId(1L))
            assertThat(entryTokenRepository.find(UserId(1L))).isNull()
        }
    }

    @Nested
    @DisplayName("처리량 초과")
    inner class ThroughputExceeded {

        @Test
        @DisplayName("배치 크기 이상 대기 시 스케줄러 1회 실행으로 배치 크기만큼만 토큰이 발급된다")
        fun exceedBatchSize_onlyBatchSizeTokensIssued() {
            // arrange — 10명 진입 (batchSize = 5)
            (1L..10L).forEach { enterQueueUseCase.execute(it) }

            // act — 스케줄러 1회 실행
            issueEntryTokensUseCase.execute()

            // assert — 앞 5명만 토큰 발급, 나머지 대기열 잔류
            val issuedCount = (1L..10L).count { entryTokenRepository.find(UserId(it)) != null }
            assertThat(issuedCount).isEqualTo(5)
            assertThat(waitingQueueRepository.count()).isEqualTo(5)

            // act — 스케줄러 2회차 실행
            issueEntryTokensUseCase.execute()

            // assert — 전원 토큰 발급, 대기열 비어있음
            val totalIssued = (1L..10L).count { entryTokenRepository.find(UserId(it)) != null }
            assertThat(totalIssued).isEqualTo(10)
            assertThat(waitingQueueRepository.count()).isZero()
        }
    }

    @Nested
    @DisplayName("토큰 만료 후 재진입")
    inner class ReEntryAfterExpiration {

        @Test
        @DisplayName("토큰 만료 후 재진입 시 맨 뒤 순번이 배정된다")
        fun reEntry_assignedToBack() {
            // arrange — 3명 진입, user1만 토큰 발급
            (1L..3L).forEach { enterQueueUseCase.execute(it) }
            val singleBatchIssueUseCase = IssueEntryTokensUseCase(
                waitingQueueRepository,
                defaultProperties.copy(batchSize = 1),
            )
            singleBatchIssueUseCase.execute()

            // act — user1 토큰 만료 시뮬레이션 → 추가 유저 → user1 재진입
            entryTokenRepository.delete(UserId(1L))
            enterQueueUseCase.execute(4L)
            val reEntryResult = enterQueueUseCase.execute(1L)

            // assert — user1은 user2, user3, user4보다 뒤에 배정
            val otherPositions = listOf(2L, 3L, 4L).map {
                getQueuePositionUseCase.execute(it).position
            }
            assertThat(reEntryResult.position).isGreaterThan(otherPositions.max())
        }
    }
}
