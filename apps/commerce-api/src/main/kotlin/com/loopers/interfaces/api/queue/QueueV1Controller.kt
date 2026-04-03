package com.loopers.interfaces.api.queue

import com.loopers.application.queue.QueueService
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.config.auth.AuthenticatedMember
import com.loopers.interfaces.config.auth.MemberAuthenticated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/queue")
class QueueV1Controller(
    private val queueService: QueueService,
) : QueueV1ApiSpec {

    @MemberAuthenticated
    @PostMapping("/enter")
    override fun enterQueue(
        authenticatedMember: AuthenticatedMember,
    ): ApiResponse<QueueV1Dto.EnterResponse> {
        val info = queueService.enterQueue(authenticatedMember.id)
        return ApiResponse.success(QueueV1Dto.EnterResponse.from(info))
    }

    @MemberAuthenticated
    @GetMapping("/position")
    override fun getPosition(
        authenticatedMember: AuthenticatedMember,
    ): ApiResponse<QueueV1Dto.PositionResponse> {
        val info = queueService.getPosition(authenticatedMember.id)
        return ApiResponse.success(QueueV1Dto.PositionResponse.from(info))
    }
}
