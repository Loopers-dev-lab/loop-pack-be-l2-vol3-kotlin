package com.loopers.interfaces.config.auth

import com.loopers.application.error.ApplicationException
import com.loopers.application.queue.QueueService
import com.loopers.domain.queue.QueueErrorType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class QueueTokenInterceptor(
    private val queueService: QueueService,
) : HandlerInterceptor {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val HEADER_QUEUE_TOKEN = "X-Loopers-QueueToken"
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (handler !is HandlerMethod) return true

        val hasAnnotation = handler.getMethodAnnotation(QueueTokenRequired::class.java) != null
        if (!hasAnnotation) return true

        if (!queueService.isEnabled()) return true

        val authenticatedMember = request.getAttribute(
            MemberAuthenticationInterceptor.AUTHENTICATED_MEMBER_ATTRIBUTE,
        ) as? AuthenticatedMember

        if (authenticatedMember == null) {
            throw ApplicationException(httpStatus = 401, code = "Unauthorized", message = "인증 정보가 없습니다.")
        }

        val token = request.getHeader(HEADER_QUEUE_TOKEN)
        if (token.isNullOrBlank()) {
            throw ApplicationException(
                httpStatus = 403,
                code = QueueErrorType.INVALID_TOKEN.code,
                message = QueueErrorType.INVALID_TOKEN.message,
            )
        }

        val valid = try {
            queueService.validateToken(authenticatedMember.id, token)
        } catch (e: Exception) {
            log.error("[QueueTokenInterceptor] 토큰 검증 실패 (memberId={})", authenticatedMember.id, e)
            throw ApplicationException(httpStatus = 503, code = "Service Unavailable", message = "대기열 서비스를 사용할 수 없습니다.")
        }

        if (!valid) {
            throw ApplicationException(
                httpStatus = 403,
                code = QueueErrorType.INVALID_TOKEN.code,
                message = QueueErrorType.INVALID_TOKEN.message,
            )
        }

        return true
    }
}
