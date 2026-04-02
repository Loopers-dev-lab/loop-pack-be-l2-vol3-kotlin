package com.loopers.interfaces.api.queue

import com.loopers.application.queue.QueueFacade
import com.loopers.interfaces.common.ApiResponse
import com.loopers.support.auth.AuthenticatedUser
import com.loopers.support.auth.AuthenticatedUserInfo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/queue")
class QueueController(
    private val queueFacade: QueueFacade,
) : QueueApiSpec {

    @PostMapping("/enter")
    override fun enterQueue(
        @AuthenticatedUser userInfo: AuthenticatedUserInfo,
    ): ApiResponse<QueueDto.EnterQueueResponse> {
        val info = queueFacade.enterQueue(userInfo.id)
        return ApiResponse.success(QueueDto.EnterQueueResponse.from(info))
    }

    @GetMapping("/position")
    override fun getPosition(
        @AuthenticatedUser userInfo: AuthenticatedUserInfo,
    ): ApiResponse<QueueDto.QueuePositionResponse> {
        val info = queueFacade.getPosition(userInfo.id)
        return ApiResponse.success(QueueDto.QueuePositionResponse.from(info))
    }
}
