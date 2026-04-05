package com.loopers.support.aspect

import com.loopers.domain.queue.QueueRepository
import com.loopers.support.annotation.WaitingQueue
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
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

class WaitingQueueAspectFailureTest {

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

    @DisplayName("토큰 검증 실패 케이스")
    @Nested
    inner class TokenValidationFailureTest {

        @Test
        fun `빈 토큰 헤더로 요청 시 즉시 ENTRY_TOKEN_MISSING 예외 발생`() {
            // arrange: 헤더 없음

            // act & assert
            val exception = org.junit.jupiter.api.assertThrows<CoreException> {
                aspect.validateEntryToken(mockk(), waitingQueue)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.ENTRY_TOKEN_MISSING)
            // verify: 토큰 조회 시도도 없음 (조기 실패)
            verify(exactly = 0) { queueRepository.getAndConsume(any(), any()) }
        }

        @Test
        fun `Redis에서 토큰을 찾을 수 없으면 ENTRY_TOKEN_INVALID 예외 발생`() {
            // arrange
            val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
            (request as MockHttpServletRequest).addHeader(WaitingQueueAspect.ENTRY_TOKEN_HEADER, validToken)
            every { queueRepository.getAndConsume(queueName, userId) } returns null

            // act & assert
            val exception = org.junit.jupiter.api.assertThrows<CoreException> {
                aspect.validateEntryToken(mockk(), waitingQueue)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.ENTRY_TOKEN_INVALID)
            // verify: 토큰 조회 (getAndConsume) 시도는 했지만 일치 체크는 없음
            verify { queueRepository.getAndConsume(queueName, userId) }
        }

        @Test
        fun `토큰 불일치 시 ENTRY_TOKEN_INVALID 예외 발생`() {
            // arrange
            val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
            (request as MockHttpServletRequest).addHeader(WaitingQueueAspect.ENTRY_TOKEN_HEADER, "wrong-token")
            every { queueRepository.getAndConsume(queueName, userId) } returns validToken

            // act & assert
            val exception = org.junit.jupiter.api.assertThrows<CoreException> {
                aspect.validateEntryToken(mockk(), waitingQueue)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.ENTRY_TOKEN_INVALID)
        }
    }

    @DisplayName("예외 발생 시 정리 동작")
    @Nested
    inner class ExceptionHandlingTest {

        @Test
        fun `메서드 실행 중 예외 발생하면 토큰은 이미 소비되었고 finally에서 정리됨`() {
            // arrange
            val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
            (request as MockHttpServletRequest).addHeader(WaitingQueueAspect.ENTRY_TOKEN_HEADER, validToken)
            every { queueRepository.getAndConsume(queueName, userId) } returns validToken
            every { queueRepository.remove(queueName, userId) } returns Unit

            val pjp = mockk<org.aspectj.lang.ProceedingJoinPoint>()
            every { pjp.proceed() } throws IllegalStateException("주문 처리 중 오류")

            // act & assert
            org.junit.jupiter.api.assertThrows<IllegalStateException> {
                aspect.validateEntryToken(pjp, waitingQueue)
            }

            // verify: 토큰은 원자적으로 소비되고, finally 블록에서 항상 정리됨 (remove 호출됨)
            verify { queueRepository.getAndConsume(queueName, userId) }
            verify { queueRepository.remove(queueName, userId) }
        }

        @Test
        fun `정상 실행 후 예외 없으면 토큰이 소비되고 대기열이 정리된다`() {
            // arrange
            val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
            (request as MockHttpServletRequest).addHeader(WaitingQueueAspect.ENTRY_TOKEN_HEADER, validToken)
            every { queueRepository.getAndConsume(queueName, userId) } returns validToken

            val pjp = mockk<org.aspectj.lang.ProceedingJoinPoint>()
            every { pjp.proceed() } returns "success"
            every { queueRepository.remove(queueName, userId) } returns Unit

            // act
            val result = aspect.validateEntryToken(pjp, waitingQueue)

            // assert
            assertThat(result).isEqualTo("success")
            // verify: 토큰은 원자적으로 소비되고 대기열이 정리됨
            verify { queueRepository.getAndConsume(queueName, userId) }
            verify { queueRepository.remove(queueName, userId) }
        }
    }
}
