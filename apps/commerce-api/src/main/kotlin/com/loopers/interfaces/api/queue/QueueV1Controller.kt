package com.loopers.interfaces.api.queue

import com.loopers.application.queue.EnterQueueUseCase
import com.loopers.application.queue.GetQueuePositionUseCase
import com.loopers.interfaces.api.queue.dto.QueueV1Dto
import com.loopers.interfaces.api.queue.spec.QueueV1ApiSpec
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.auth.AuthUser
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/queue")
class QueueV1Controller(
    private val enterQueueUseCase: EnterQueueUseCase,
    private val getQueuePositionUseCase: GetQueuePositionUseCase,
) : QueueV1ApiSpec {

    @PostMapping("/enter")
    override fun enterQueue(
        @AuthUser userId: Long,
    ): ApiResponse<QueueV1Dto.QueuePositionResponse> {
        return enterQueueUseCase.execute(userId)
            .let { QueueV1Dto.QueuePositionResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/position")
    override fun getQueuePosition(
        @AuthUser userId: Long,
    ): ApiResponse<QueueV1Dto.QueuePositionResponse> {
        return getQueuePositionUseCase.execute(userId)
            .let { QueueV1Dto.QueuePositionResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
