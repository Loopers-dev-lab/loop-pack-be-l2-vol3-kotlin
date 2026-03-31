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
        val userId = request.getAttribute(ATTRIBUTE_USER_ID) as Long
        val headerToken = request.getHeader(HEADER_ENTRY_TOKEN)

        if (headerToken == null) {
            if (queueFallbackHandler.isAvailable()) {
                throw CoreException(ErrorType.FORBIDDEN, "입장 토큰이 필요합니다.")
            }
            return true
        }

        return try {
            validateEntryTokenUseCase.execute(userId, headerToken)
            queueFallbackHandler.markAvailable()
            true
        } catch (e: CoreException) {
            throw e
        } catch (e: Exception) {
            queueFallbackHandler.markUnavailable(e.message ?: "Redis 연결 실패")
            true
        }
    }
}
