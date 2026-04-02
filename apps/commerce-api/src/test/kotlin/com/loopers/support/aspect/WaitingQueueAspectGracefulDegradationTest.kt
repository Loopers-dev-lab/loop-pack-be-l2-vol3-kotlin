package com.loopers.support.aspect

import com.loopers.domain.queue.QueueRepository
import com.loopers.support.annotation.WaitingQueue
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
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

    @DisplayName("정리(Cleanup) 단계 - Redis 장애 시 로그만 남기고 진행")
    @Nested
    inner class CleanupGracefulDegradationTest {

        @Test
        fun `deleteToken Redis 실패해도 결과 반환한다`() {
            // arrange
            val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
            (request as MockHttpServletRequest).addHeader(WaitingQueueAspect.ENTRY_TOKEN_HEADER, validToken)
            every { queueRepository.getToken(queueName, userId) } returns validToken

            val pjp = mockk<org.aspectj.lang.ProceedingJoinPoint>()
            every { pjp.proceed() } returns "success"
            every { queueRepository.deleteToken(queueName, userId) } throws RuntimeException("Redis 연결 실패")
            every { queueRepository.remove(queueName, userId) } returns Unit

            // act
            val result = aspect.validateEntryToken(pjp, waitingQueue)

            // assert: 결과 반환됨 (로그만 남김)
            assertThat(result).isEqualTo("success")
            verify { queueRepository.getToken(queueName, userId) }
            verify { queueRepository.deleteToken(queueName, userId) }
        }

        @Test
        fun `remove Redis 실패해도 결과 반환한다`() {
            // arrange
            val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
            (request as MockHttpServletRequest).addHeader(WaitingQueueAspect.ENTRY_TOKEN_HEADER, validToken)
            every { queueRepository.getToken(queueName, userId) } returns validToken

            val pjp = mockk<org.aspectj.lang.ProceedingJoinPoint>()
            every { pjp.proceed() } returns "success"
            every { queueRepository.deleteToken(queueName, userId) } returns Unit
            every { queueRepository.remove(queueName, userId) } throws RuntimeException("Redis 연결 실패")

            // act
            val result = aspect.validateEntryToken(pjp, waitingQueue)

            // assert: 결과 반환됨 (로그만 남김)
            assertThat(result).isEqualTo("success")
            verify { queueRepository.deleteToken(queueName, userId) }
            verify { queueRepository.remove(queueName, userId) }
        }

        @Test
        fun `deleteToken과 remove 모두 Redis 실패해도 결과 반환한다`() {
            // arrange
            val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
            (request as MockHttpServletRequest).addHeader(WaitingQueueAspect.ENTRY_TOKEN_HEADER, validToken)
            every { queueRepository.getToken(queueName, userId) } returns validToken

            val pjp = mockk<org.aspectj.lang.ProceedingJoinPoint>()
            every { pjp.proceed() } returns "success"
            every { queueRepository.deleteToken(queueName, userId) } throws RuntimeException("Redis 연결 실패")
            every { queueRepository.remove(queueName, userId) } throws RuntimeException("Redis 연결 실패")

            // act
            val result = aspect.validateEntryToken(pjp, waitingQueue)

            // assert: 결과 반환됨 (첫 번째 실패로 로그, 제거는 실행 안 됨)
            assertThat(result).isEqualTo("success")
        }
    }
}
