package com.loopers.support.aspect

import com.loopers.domain.queue.QueueRepository
import com.loopers.support.annotation.WaitingQueue
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Aspect
@Component
class WaitingQueueAspect(
    private val queueRepository: QueueRepository,
) {
    private val log = LoggerFactory.getLogger(WaitingQueueAspect::class.java)

    companion object {
        const val ENTRY_TOKEN_HEADER = "X-Entry-Token"
    }

    @Around("@annotation(waitingQueue)")
    fun validateEntryToken(pjp: ProceedingJoinPoint, waitingQueue: WaitingQueue): Any? {
        val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
        val userId = request.getAttribute("userId") as Long

        val token = request.getHeader(ENTRY_TOKEN_HEADER)
            ?: throw CoreException(ErrorType.ENTRY_TOKEN_REQUIRED)

        val storedToken = queueRepository.getToken(waitingQueue.name, userId)
            ?: throw CoreException(ErrorType.ENTRY_TOKEN_INVALID)

        if (token != storedToken) {
            throw CoreException(ErrorType.ENTRY_TOKEN_INVALID)
        }

        val result = pjp.proceed()

        runCatching {
            queueRepository.deleteToken(waitingQueue.name, userId)
            queueRepository.remove(waitingQueue.name, userId)
        }.onFailure { e ->
            log.warn(
                "[WaitingQueueAspect] 토큰 정리 실패 (TTL 대기). queueName={}, userId={}",
                waitingQueue.name,
                userId,
                e,
            )
        }

        return result
    }
}
