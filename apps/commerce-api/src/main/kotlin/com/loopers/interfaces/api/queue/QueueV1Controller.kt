package com.loopers.interfaces.api.queue

import com.loopers.application.queue.EnterQueueCriteria
import com.loopers.application.queue.GetPositionCriteria
import com.loopers.application.queue.QueueFacade
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.auth.CurrentUser
import com.loopers.support.auth.LoginUser
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/queue")
class QueueV1Controller(
    private val queueFacade: QueueFacade,
) : QueueV1ApiSpec {

    @PostMapping("/enter")
    override fun enterQueue(
        @CurrentUser loginUser: LoginUser,
    ): ApiResponse<QueueV1Dto.EnterQueueResponse> {
        return queueFacade.enterQueue(EnterQueueCriteria(loginUser.id))
            .let { QueueV1Dto.EnterQueueResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/position")
    override fun getPosition(
        @CurrentUser loginUser: LoginUser,
    ): ApiResponse<QueueV1Dto.QueuePositionResponse> {
        return queueFacade.getPosition(GetPositionCriteria(loginUser.id))
            .let { QueueV1Dto.QueuePositionResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
