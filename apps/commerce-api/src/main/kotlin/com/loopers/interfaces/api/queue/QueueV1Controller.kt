package com.loopers.interfaces.api.queue

import com.loopers.application.queue.QueueFacade
import com.loopers.interfaces.api.ApiResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/queues")
class QueueV1Controller(
    private val queueFacade: QueueFacade,
    @Value("\${queue.throughput-per-server-per-second:175}")
    private val throughputPerServerPerSecond: Int,
) : QueueV1ApiSpec {

    @PostMapping("/{queueName}/enter")
    @ResponseStatus(HttpStatus.CREATED)
    override fun enter(
        @PathVariable queueName: String,
        @RequestAttribute("userId") userId: Long,
    ): ApiResponse<QueueV1Dto.EnterResponse> {
        val info = queueFacade.enter(
            queueName = queueName,
            userId = userId,
            throughputPerServerPerSecond = throughputPerServerPerSecond,
        )
        return ApiResponse.success(
            QueueV1Dto.EnterResponse(
                queueName = info.queueName,
                position = info.position,
                estimatedWaitSeconds = info.estimatedWaitSeconds,
            ),
        )
    }

    @GetMapping("/{queueName}/position")
    override fun getPosition(
        @PathVariable queueName: String,
        @RequestAttribute("userId") userId: Long,
    ): ApiResponse<QueueV1Dto.PositionResponse> {
        val info = queueFacade.getPosition(
            queueName = queueName,
            userId = userId,
            throughputPerServerPerSecond = throughputPerServerPerSecond,
        )
        return ApiResponse.success(
            QueueV1Dto.PositionResponse(
                queueName = info.queueName,
                position = info.position,
                estimatedWaitSeconds = info.estimatedWaitSeconds,
                token = info.token,
            ),
        )
    }

    @GetMapping("/{queueName}/status")
    override fun getStatus(
        @PathVariable queueName: String,
    ): ApiResponse<QueueV1Dto.StatusResponse> {
        val info = queueFacade.getStatus(
            queueName = queueName,
            throughputPerServerPerSecond = throughputPerServerPerSecond,
        )
        return ApiResponse.success(
            QueueV1Dto.StatusResponse(
                queueName = info.queueName,
                totalWaiting = info.totalWaiting,
                throughputPerSecond = info.throughputPerSecond,
            ),
        )
    }
}
