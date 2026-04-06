package com.loopers.interfaces.api.queue

import com.loopers.application.queue.QueueFacade
import com.loopers.support.auth.AuthenticatedUser
import com.loopers.support.auth.AuthenticatedUserInfo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/api/v1/queue")
class QueueStreamController(
    private val queueFacade: QueueFacade,
) : QueueStreamApiSpec {

    @GetMapping("/stream")
    override fun stream(
        @AuthenticatedUser userInfo: AuthenticatedUserInfo,
    ): SseEmitter {
        return queueFacade.subscribe(userInfo.id)
    }
}
