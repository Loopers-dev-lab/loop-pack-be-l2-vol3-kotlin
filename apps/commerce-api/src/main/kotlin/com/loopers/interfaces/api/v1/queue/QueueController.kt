package com.loopers.interfaces.api.v1.queue

import com.loopers.application.queue.EnterQueueUseCase
import com.loopers.application.queue.GetQueuePositionUseCase
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AuthUser
import com.loopers.interfaces.api.auth.AuthenticatedUser
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/queue")
class QueueController(
    private val enterQueueUseCase: EnterQueueUseCase,
    private val getQueuePositionUseCase: GetQueuePositionUseCase,
) {
    @PostMapping("/enter")
    fun enterQueue(
        @AuthenticatedUser authUser: AuthUser,
    ): ApiResponse<QueueEnterResponse> {
        val result = enterQueueUseCase.enter(authUser.id)
        return ApiResponse.success(QueueEnterResponse.from(result))
    }

    @GetMapping("/position")
    fun getPosition(
        @AuthenticatedUser authUser: AuthUser,
    ): ApiResponse<QueuePositionResponse> {
        val result = getQueuePositionUseCase.getPosition(authUser.id)
        return ApiResponse.success(QueuePositionResponse.from(result))
    }
}
