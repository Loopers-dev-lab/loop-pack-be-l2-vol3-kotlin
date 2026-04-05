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
        val attributes = RequestContextHolder.getRequestAttributes()
            ?: throw CoreException(ErrorType.INTERNAL_ERROR, "RequestContext not found - missing RequestContextFilter")

        val servletAttributes = attributes as? ServletRequestAttributes
            ?: throw CoreException(ErrorType.INTERNAL_ERROR, "RequestAttributes is not ServletRequestAttributes")

        val request = servletAttributes.request

        val userIdObj = request.getAttribute("userId")
            ?: throw CoreException(
                ErrorType.INTERNAL_ERROR,
                "userId attribute not found in request - missing authentication interceptor",
            )

        val userId = when (userIdObj) {
            is Long -> userIdObj
            is Number -> userIdObj.toLong()
            else -> throw CoreException(
                ErrorType.INTERNAL_ERROR,
                "userId must be a Long or Number, got ${userIdObj::class.simpleName}",
            )
        }

        val token = request.getHeader(ENTRY_TOKEN_HEADER)
            ?: throw CoreException(ErrorType.ENTRY_TOKEN_MISSING)

        // 토큰을 원자적으로 소비 (GETDEL): 성공 시 토큰이 즉시 제거되므로 재사용 불가
        val storedToken = queueRepository.getAndConsume(waitingQueue.name, userId)
            ?: throw CoreException(ErrorType.ENTRY_TOKEN_INVALID)

        if (token != storedToken) {
            throw CoreException(ErrorType.ENTRY_TOKEN_INVALID)
        }

        try {
            return pjp.proceed()
        } finally {
            // 정리 단계: 토큰은 이미 소비되었으므로, 정상/예외 상관없이 항상 대기열에서 사용자 제거
            // 이를 통해 토큰이 남아있어 재시도 시 대기열을 우회하는 문제 방지
            runCatching {
                queueRepository.remove(waitingQueue.name, userId)
            }.onFailure { e ->
                log.warn(
                    "[WaitingQueueAspect] 대기열 제거 실패 (TTL 대기). queueName={}, userId={}",
                    waitingQueue.name,
                    userId,
                    e,
                )
            }
        }
    }
}
