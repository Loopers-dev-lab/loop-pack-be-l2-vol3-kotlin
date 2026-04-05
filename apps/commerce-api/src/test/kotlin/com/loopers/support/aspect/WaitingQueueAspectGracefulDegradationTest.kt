package com.loopers.support.aspect

import com.loopers.domain.queue.QueueRepository
import com.loopers.support.annotation.WaitingQueue
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

class WaitingQueueAspectGracefulDegradationTest {

    private lateinit var aspect: WaitingQueueAspect
    private lateinit var queueRepository: QueueRepository
    private lateinit var waitingQueue: WaitingQueue

    private val queueName = "order-queue"
    private val userId = 100L
    private val validToken = "valid-token-abc"

    @BeforeEach
    fun setUp() {
        queueRepository = mockk()
        aspect = WaitingQueueAspect(queueRepository)

        waitingQueue = mockk()
        every { waitingQueue.name } returns queueName

        // RequestContextHolder에 Mock 요청 등록
        val request = MockHttpServletRequest()
        request.setAttribute("userId", userId)
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
    }

    @AfterEach
    fun tearDown() {
        // RequestContextHolder 초기화: setUp()에서 생성한 mock request 제거
        RequestContextHolder.resetRequestAttributes()
    }

    @DisplayName("정리(Cleanup) 단계 - Redis 장애 시 로그만 남기고 진행")
    @Nested
    inner class CleanupGracefulDegradationTest {

        @Test
        fun `remove Redis 실패해도 토큰은 이미 소비되어 재사용 불가`() {
            // arrange
            val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
            (request as MockHttpServletRequest).addHeader(WaitingQueueAspect.ENTRY_TOKEN_HEADER, validToken)

            // 첫 번째 호출: getAndConsume이 성공 (토큰 원자적으로 소비)
            every { queueRepository.getAndConsume(queueName, userId) } returns validToken
            val pjp1 = mockk<org.aspectj.lang.ProceedingJoinPoint>()
            every { pjp1.proceed() } returns "success"
            every { queueRepository.remove(queueName, userId) } throws RuntimeException("Redis 연결 실패")

            // act: 첫 번째 호출 - 성공
            val result = aspect.validateEntryToken(pjp1, waitingQueue)

            // assert: 결과 반환됨, getAndConsume 호출됨
            assertThat(result).isEqualTo("success")
            verify { queueRepository.getAndConsume(queueName, userId) }
            verify { queueRepository.remove(queueName, userId) }

            // 두 번째 호출: getAndConsume이 null 반환 (토큰이 이미 소비됨)
            every { queueRepository.getAndConsume(queueName, userId) } returns null
            val pjp2 = mockk<org.aspectj.lang.ProceedingJoinPoint>()

            // act & assert: 두 번째 호출 - 실패 (토큰이 없음)
            val exception = org.junit.jupiter.api.assertThrows<com.loopers.support.error.CoreException> {
                aspect.validateEntryToken(pjp2, waitingQueue)
            }
            assertThat(exception.errorType).isEqualTo(com.loopers.support.error.ErrorType.ENTRY_TOKEN_INVALID)
            verify(exactly = 2) { queueRepository.getAndConsume(queueName, userId) }
        }

        @Test
        fun `remove Redis 실패해도 결과 반환한다`() {
            // arrange
            val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
            (request as MockHttpServletRequest).addHeader(WaitingQueueAspect.ENTRY_TOKEN_HEADER, validToken)
            every { queueRepository.getAndConsume(queueName, userId) } returns validToken

            val pjp = mockk<org.aspectj.lang.ProceedingJoinPoint>()
            every { pjp.proceed() } returns "success"
            every { queueRepository.remove(queueName, userId) } throws RuntimeException("Redis 연결 실패")

            // act
            val result = aspect.validateEntryToken(pjp, waitingQueue)

            // assert: 결과 반환됨 (로그만 남김), getAndConsume으로 토큰 이미 소비됨
            assertThat(result).isEqualTo("success")
            verify { queueRepository.getAndConsume(queueName, userId) }
            verify { queueRepository.remove(queueName, userId) }
        }
    }
}
