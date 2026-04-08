package com.loopers.interfaces.api.queue

import com.loopers.application.queue.QueueFacade
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/queue")
class QueueV1Controller(
    private val queueFacade: QueueFacade,
) : QueueV1ApiSpec {
    @PostMapping("/enter")
    override fun enterQueue(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
    ): ApiResponse<QueueV1Dto.EnterResponse> {
        return queueFacade.enterQueue(loginId, password)
            .let { QueueV1Dto.EnterResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/position")
    override fun getPosition(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
    ): ApiResponse<QueueV1Dto.PositionResponse> {
        return queueFacade.getPosition(loginId, password)
            .let { QueueV1Dto.PositionResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
