package com.loopers.interfaces.api.queue

import com.loopers.application.queue.QueueFacade
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/queue")
class QueueAdminV1Controller(
    private val queueFacade: QueueFacade,
) : QueueAdminV1ApiSpec {

    @PostMapping("/enable")
    override fun enableQueue(): ApiResponse<QueueAdminV1Dto.QueueStatusResponse> {
        return queueFacade.enableQueue()
            .let { QueueAdminV1Dto.QueueStatusResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PostMapping("/disable")
    override fun disableQueue(): ApiResponse<QueueAdminV1Dto.QueueStatusResponse> {
        return queueFacade.disableQueue()
            .let { QueueAdminV1Dto.QueueStatusResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/status")
    override fun getQueueStatus(): ApiResponse<QueueAdminV1Dto.QueueStatusResponse> {
        return queueFacade.getQueueStatus()
            .let { QueueAdminV1Dto.QueueStatusResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
