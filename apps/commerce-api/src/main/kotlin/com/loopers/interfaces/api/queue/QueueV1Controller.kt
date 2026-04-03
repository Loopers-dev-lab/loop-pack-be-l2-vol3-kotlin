package com.loopers.interfaces.api.queue

import com.loopers.application.auth.AuthUseCase
import com.loopers.application.queue.QueueExperimentUseCase
import com.loopers.application.queue.QueueStrategyType
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/queue")
class QueueV1Controller(
    private val authUseCase: AuthUseCase,
    private val queueExperimentUseCase: QueueExperimentUseCase,
) : QueueV1ApiSpec {
    @PostMapping("/enter")
    override fun enter(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @RequestBody request: QueueV1Dto.EnterRequest,
    ): ApiResponse<QueueV1Dto.PositionResponse> {
        val member = authUseCase.authenticate(loginId, password)
        return queueExperimentUseCase.enter(member.id!!, request.strategy)
            .let(QueueV1Dto.PositionResponse::from)
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/position")
    override fun getPosition(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @RequestParam(required = false) strategy: QueueStrategyType?,
    ): ApiResponse<QueueV1Dto.PositionResponse> {
        val member = authUseCase.authenticate(loginId, password)
        return queueExperimentUseCase.getStatus(member.id!!, strategy)
            .let(QueueV1Dto.PositionResponse::from)
            .let { ApiResponse.success(it) }
    }
}
