package com.loopers.interfaces.api.admin.queue

import com.loopers.application.queue.QueueService
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.config.auth.AdminAuthenticated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/queue")
@AdminAuthenticated
class AdminQueueV1Controller(
    private val queueService: QueueService,
) : AdminQueueV1ApiSpec {

    @PostMapping("/toggle")
    override fun toggleQueue(
        @RequestBody request: AdminQueueV1Dto.ToggleRequest,
    ): ApiResponse<AdminQueueV1Dto.ToggleResponse> {
        queueService.setEnabled(request.enabled)
        return ApiResponse.success(AdminQueueV1Dto.ToggleResponse(enabled = request.enabled))
    }

    @GetMapping("/status")
    override fun getQueueStatus(): ApiResponse<AdminQueueV1Dto.StatusResponse> {
        val status = queueService.getStatus()
        return ApiResponse.success(AdminQueueV1Dto.StatusResponse.from(status))
    }
}
