package com.loopers.interfaces.api.queue

import com.loopers.support.auth.AuthenticatedUserInfo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Tag(name = "Queue Stream API", description = "대기열 SSE 스트림 API")
interface QueueStreamApiSpec {

    @Operation(
        summary = "대기열 SSE 스트림",
        description = "SSE 연결을 통해 실시간 대기 순번 변화를 수신합니다.",
    )
    fun stream(userInfo: AuthenticatedUserInfo): SseEmitter
}
