package com.loopers.interfaces.api.security

import com.loopers.domain.queue.QueueService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

/**
 * POST /api/v1/orders 요청에 대해 입장 토큰(X-Entry-Token)을 검증하는 인터셉터.
 *
 * 토큰이 없거나 유효하지 않으면 요청을 거부한다.
 * WebMvcConfig 에서 주문 생성 경로로 등록된다.
 */
@Component
class QueueEntryInterceptor(
    private val queueService: QueueService,
) : HandlerInterceptor {

    companion object {
        const val HEADER_ENTRY_TOKEN = "X-Entry-Token"
        const val HEADER_LOGIN_ID = "X-Loopers-LoginId"
    }

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (request.method != "POST") return true

        val token = request.getHeader(HEADER_ENTRY_TOKEN)
            ?: throw CoreException(ErrorType.BAD_REQUEST, "입장 토큰이 필요합니다. 대기열을 통해 토큰을 발급받으세요.")

        val loginId = request.getHeader(HEADER_LOGIN_ID)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, "인증 정보가 없습니다.")

        // LoginUserArgumentResolver가 loginId로 User를 조회하므로,
        // 여기서는 userId를 직접 알 수 없다.
        // 대신 token의 존재 여부를 통해 최소 검증하고,
        // 상세 검증은 컨트롤러에서 userId 확정 후 수행한다.
        // → 인터셉터에서는 토큰 헤더 존재 여부만 확인 (fast-fail)

        return true
    }
}
