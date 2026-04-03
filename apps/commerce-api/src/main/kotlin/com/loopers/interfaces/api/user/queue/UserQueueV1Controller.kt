package com.loopers.interfaces.api.user.queue

import com.loopers.application.user.auth.UserAuthenticateUseCase
import com.loopers.application.user.queue.QueueCommand
import com.loopers.application.user.queue.QueueEnterUseCase
import com.loopers.application.user.queue.QueuePositionUseCase
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/queue")
@RestController
class UserQueueV1Controller(
    private val userAuthenticateUseCase: UserAuthenticateUseCase,
    private val queueEnterUseCase: QueueEnterUseCase,
    private val queuePositionUseCase: QueuePositionUseCase,
) : UserQueueV1ApiSpec {
    @PostMapping("/enter")
    override fun enter(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
    ): ApiResponse<UserQueueV1Response.Enter> {
        val userId = userAuthenticateUseCase.authenticateAndGetId(loginId, password)
        return queueEnterUseCase.enter(QueueCommand.Enter(userId))
            .let { UserQueueV1Response.Enter.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/position")
    override fun getPosition(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
    ): ApiResponse<UserQueueV1Response.Position> {
        val userId = userAuthenticateUseCase.authenticateAndGetId(loginId, password)
        return queuePositionUseCase.getPosition(QueueCommand.Position(userId))
            .let { UserQueueV1Response.Position.from(it) }
            .let { ApiResponse.success(it) }
    }
}
