package com.loopers.application.user

import com.loopers.domain.user.event.UserActionEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * 유저 행동 로깅 이벤트 핸들러.
 *
 * 조회, 좋아요, 주문 등 유저 행동을 비동기로 로깅한다.
 * - @Async: 별도 스레드에서 실행하여 원본 요청 응답에 영향 없음
 * - @EventListener: 트랜잭션과 무관하게 이벤트 수신 (행동 로깅은 트랜잭션 결과와 독립)
 */
@Component
class UserActionLogHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @EventListener
    fun onUserAction(event: UserActionEvent) {
        log.info(
            "[유저 행동] userId={}, action={}, targetId={}, timestamp={}",
            event.userId,
            event.actionType,
            event.targetId,
            event.timestamp,
        )
    }
}
