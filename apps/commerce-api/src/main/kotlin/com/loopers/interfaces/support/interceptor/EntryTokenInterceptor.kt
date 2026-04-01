package com.loopers.interfaces.support.interceptor

import com.loopers.application.queue.QueueFallbackHandler
import com.loopers.application.queue.ValidateEntryTokenUseCase
import com.loopers.interfaces.support.ATTRIBUTE_USER_ID
import com.loopers.interfaces.support.HEADER_ENTRY_TOKEN
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class EntryTokenInterceptor(
    private val validateEntryTokenUseCase: ValidateEntryTokenUseCase,
    private val queueFallbackHandler: QueueFallbackHandler,
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (request.method != "POST") return true

        val userId = request.getAttribute(ATTRIBUTE_USER_ID) as? Long
            ?: throw CoreException(ErrorType.UNAUTHORIZED, "인증 정보가 유효하지 않습니다.")

        if (!queueFallbackHandler.isAvailable()) {
            throw CoreException(ErrorType.SERVICE_UNAVAILABLE, "현재 대기열 서비스를 이용할 수 없습니다.")
        }

        val headerToken = request.getHeader(HEADER_ENTRY_TOKEN)
            ?: throw CoreException(ErrorType.FORBIDDEN, "입장 토큰이 필요합니다.")

        return try {
            validateEntryTokenUseCase.execute(userId, headerToken)
            queueFallbackHandler.markAvailable()
            true
        } catch (e: CoreException) {
            throw e
        } catch (e: Exception) {
            queueFallbackHandler.markUnavailable(e.message ?: "Redis 연결 실패")
            throw CoreException(ErrorType.SERVICE_UNAVAILABLE, "현재 대기열 서비스를 이용할 수 없습니다.")
        }
    }
}
