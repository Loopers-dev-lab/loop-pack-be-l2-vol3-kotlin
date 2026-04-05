package com.loopers.support.aspect

import com.loopers.domain.queue.QueueRepository
import com.loopers.support.annotation.WaitingQueue
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.aspectj.lang.ProceedingJoinPoint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

class WaitingQueueAspectTest {

    private lateinit var aspect: WaitingQueueAspect
    private lateinit var queueRepository: QueueRepository
    private lateinit var pjp: ProceedingJoinPoint
    private lateinit var waitingQueue: WaitingQueue

    private val queueName = "order-queue"
    private val userId = 100L
    private val validToken = "valid-token-abc"

    @BeforeEach
    fun setUp() {
        queueRepository = mockk()
        pjp = mockk()
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

    @DisplayName("유효한 토큰으로 요청 시")
    @Nested
    inner class ValidTokenTest {

        @Test
        fun `메서드를 실행하고 토큰을 소비한 후 대기열을 정리한다`() {
            // arrange
            val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
            (request as MockHttpServletRequest).addHeader(WaitingQueueAspect.ENTRY_TOKEN_HEADER, validToken)

            every { queueRepository.getAndConsume(queueName, userId) } returns validToken
            every { pjp.proceed() } returns "success"
            every { queueRepository.remove(queueName, userId) } returns Unit

            // act
            val result = aspect.validateEntryToken(pjp, waitingQueue)

            // assert
            assertThat(result).isEqualTo("success")
            verify { queueRepository.getAndConsume(queueName, userId) }
            verify { queueRepository.remove(queueName, userId) }
        }
    }

    @DisplayName("토큰 헤더가 없을 때")
    @Nested
    inner class MissingTokenTest {

        @Test
        fun `ENTRY_TOKEN_MISSING 예외가 발생한다`() {
            // arrange: 헤더 없음 (기본 MockHttpServletRequest)

            // act & assert
            val exception = org.junit.jupiter.api.assertThrows<CoreException> {
                aspect.validateEntryToken(pjp, waitingQueue)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.ENTRY_TOKEN_MISSING)
        }
    }

    @DisplayName("Redis에 토큰이 없을 때")
    @Nested
    inner class StoredTokenNotFoundTest {

        @Test
        fun `ENTRY_TOKEN_INVALID 예외가 발생한다`() {
            // arrange
            val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
            (request as MockHttpServletRequest).addHeader(WaitingQueueAspect.ENTRY_TOKEN_HEADER, validToken)
            every { queueRepository.getAndConsume(queueName, userId) } returns null

            // act & assert
            val exception = org.junit.jupiter.api.assertThrows<CoreException> {
                aspect.validateEntryToken(pjp, waitingQueue)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.ENTRY_TOKEN_INVALID)
        }
    }

    @DisplayName("토큰이 불일치할 때")
    @Nested
    inner class InvalidTokenTest {

        @Test
        fun `ENTRY_TOKEN_INVALID 예외가 발생한다`() {
            // arrange
            val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
            (request as MockHttpServletRequest).addHeader(WaitingQueueAspect.ENTRY_TOKEN_HEADER, "wrong-token")
            every { queueRepository.getAndConsume(queueName, userId) } returns validToken

            // act & assert
            val exception = org.junit.jupiter.api.assertThrows<CoreException> {
                aspect.validateEntryToken(pjp, waitingQueue)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.ENTRY_TOKEN_INVALID)
        }
    }

    @DisplayName("메서드 실행 중 예외 발생 시")
    @Nested
    inner class MethodFailureTest {

        @Test
        fun `토큰은 이미 소비되고 finally에서 정리한 후 예외를 전파한다`() {
            // arrange
            val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
            (request as MockHttpServletRequest).addHeader(WaitingQueueAspect.ENTRY_TOKEN_HEADER, validToken)
            every { queueRepository.getAndConsume(queueName, userId) } returns validToken
            every { queueRepository.remove(queueName, userId) } returns Unit
            every { pjp.proceed() } throws RuntimeException("주문 처리 실패")

            // act & assert
            org.junit.jupiter.api.assertThrows<RuntimeException> {
                aspect.validateEntryToken(pjp, waitingQueue)
            }
            // 토큰은 원자적으로 소비됨 (getAndConsume 호출됨)
            verify { queueRepository.getAndConsume(queueName, userId) }
            // finally 블록에서 항상 정리됨 (remove 호출됨)
            verify { queueRepository.remove(queueName, userId) }
        }
    }
}
