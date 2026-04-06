package com.loopers.interfaces.api.orderqueue

import com.loopers.application.orderqueue.EnterQueueCriteria
import com.loopers.application.orderqueue.GetQueuePositionCriteria
import com.loopers.application.orderqueue.UserEnterOrderQueueUseCase
import com.loopers.application.orderqueue.UserGetQueuePositionUseCase
import com.loopers.interfaces.api.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/order-queue")
class OrderQueueV1Controller(
    private val userEnterOrderQueueUseCase: UserEnterOrderQueueUseCase,
    private val userGetQueuePositionUseCase: UserGetQueuePositionUseCase,
) : OrderQueueV1ApiSpec {

    @PostMapping("/enter")
    @ResponseStatus(HttpStatus.OK)
    override fun enter(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") loginPw: String,
    ): ApiResponse<OrderQueueV1Dto.EnterResponse> {
        val criteria = EnterQueueCriteria(loginId = loginId)
        val result = userEnterOrderQueueUseCase.execute(criteria)
        return ApiResponse.success(OrderQueueV1Dto.EnterResponse.from(result))
    }

    @GetMapping("/position")
    @ResponseStatus(HttpStatus.OK)
    override fun getPosition(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") loginPw: String,
    ): ApiResponse<OrderQueueV1Dto.PositionResponse> {
        val criteria = GetQueuePositionCriteria(loginId = loginId)
        val result = userGetQueuePositionUseCase.execute(criteria)
        return ApiResponse.success(OrderQueueV1Dto.PositionResponse.from(result))
    }
}
