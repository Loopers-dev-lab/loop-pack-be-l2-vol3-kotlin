package com.loopers.interfaces.api.queue

import com.loopers.application.queue.QueueFacade
import com.loopers.domain.auth.AuthenticatedMember
import com.loopers.infrastructure.auth.JwtAuthenticationFilter
import com.loopers.interfaces.api.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/queue")
class QueueV1Controller(
    private val queueFacade: QueueFacade,
) {
    @PostMapping("/enter")
    fun enterQueue(
        httpRequest: HttpServletRequest,
    ): ApiResponse<QueueV1Dto.EnterResponse> {
        val member = httpRequest.getAttribute(
            JwtAuthenticationFilter.AUTHENTICATED_MEMBER_ATTRIBUTE,
        ) as AuthenticatedMember

        val result = queueFacade.enterQueue(member.memberId)
        return ApiResponse.success(QueueV1Dto.EnterResponse.from(result))
    }

    @GetMapping("/position")
    fun getPosition(
        httpRequest: HttpServletRequest,
    ): ApiResponse<QueueV1Dto.PositionResponse> {
        val member = httpRequest.getAttribute(
            JwtAuthenticationFilter.AUTHENTICATED_MEMBER_ATTRIBUTE,
        ) as AuthenticatedMember

        val result = queueFacade.getQueuePosition(member.memberId)
        return ApiResponse.success(QueueV1Dto.PositionResponse.from(result))
    }
}
