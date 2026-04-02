package com.loopers.interfaces.api.queue

import com.loopers.application.queue.EnterQueueUseCase
import com.loopers.application.queue.GetQueuePositionUseCase
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.CurrentUserId
import com.loopers.support.constant.ApiPaths
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(ApiPaths.Queue.BASE)
class QueueV1Controller(
    private val enterQueueUseCase: EnterQueueUseCase,
    private val getQueuePositionUseCase: GetQueuePositionUseCase,
) {

    @PostMapping("/enter")
    fun enter(
        @CurrentUserId userId: Long,
    ): ApiResponse<QueueEntryResponse> {
        val info = enterQueueUseCase.execute(userId)
        return ApiResponse.success(QueueEntryResponse.from(info))
    }

    @GetMapping("/position")
    fun getPosition(
        @CurrentUserId userId: Long,
    ): ApiResponse<QueuePositionResponse> {
        val info = getQueuePositionUseCase.execute(userId)
        return ApiResponse.success(QueuePositionResponse.from(info))
    }
}
