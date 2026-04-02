package com.loopers.interfaces.api.queue

import com.loopers.application.queue.QueueProperties
import com.loopers.application.user.AuthenticateUserUseCase
import com.loopers.domain.queue.OrderQueueStore
import com.loopers.support.constant.AuthHeaders
import com.loopers.support.error.CoreException
import com.loopers.support.error.QueueErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class EntryTokenInterceptor(
    private val authenticateUserUseCase: AuthenticateUserUseCase,
    private val orderQueueStore: OrderQueueStore,
    private val queueProperties: QueueProperties,
) : HandlerInterceptor {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (!queueProperties.enabled) return true
        if (request.method != "POST") return true

        val loginId = request.getHeader(AuthHeaders.User.LOGIN_ID) ?: return true
        val password = request.getHeader(AuthHeaders.User.LOGIN_PW) ?: return true

        val userId = try {
            authenticateUserUseCase.execute(loginId, password)
        } catch (e: CoreException) {
            return true
        }

        try {
            if (!orderQueueStore.hasToken(userId)) {
                throw CoreException(QueueErrorCode.ENTRY_TOKEN_REQUIRED)
            }
        } catch (e: CoreException) {
            throw e
        } catch (e: Exception) {
            log.error("대기열 토큰 검증 실패 — Redis 연결 오류, 통과 허용 [userId={}]", userId, e)
            request.setAttribute(RESOLVED_USER_ID_ATTRIBUTE, userId)
            return true
        }

        request.setAttribute(RESOLVED_USER_ID_ATTRIBUTE, userId)
        return true
    }

    companion object {
        const val RESOLVED_USER_ID_ATTRIBUTE = "resolvedUserId"
    }
}
