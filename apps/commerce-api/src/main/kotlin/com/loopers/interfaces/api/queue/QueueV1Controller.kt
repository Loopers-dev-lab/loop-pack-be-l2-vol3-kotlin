package com.loopers.interfaces.api.queue

import com.loopers.application.queue.QueueService
import com.loopers.application.user.UserService
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class QueueV1Controller(
    private val userService: UserService,
    private val queueService: QueueService,
) : QueueV1ApiSpec {

    @PostMapping("/api/v1/queue/enter")
    override fun enterQueue(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
    ): ApiResponse<QueueV1Dto.EnterResponse> {
        val authUser = userService.authenticate(loginId, password)
        val result = queueService.enterQueue(authUser.id)
        return ApiResponse.success(QueueV1Dto.EnterResponse.from(result))
    }

    @GetMapping("/api/v1/queue/position")
    override fun getPosition(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
    ): ApiResponse<QueueV1Dto.PositionResponse> {
        val authUser = userService.authenticate(loginId, password)
        val result = queueService.getQueuePosition(authUser.id)
        return ApiResponse.success(QueueV1Dto.PositionResponse.from(result))
    }
}
