package com.loopers.application.event

import com.loopers.config.async.AsyncConfig
import com.loopers.domain.event.UserActionEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 유저 행동 로깅 리스너.
 *
 * 모든 유저 행동 이벤트를 수신하여 로그로 기록한다.
 * 실패해도 비즈니스에 영향 없음. 향후 행동 분석용 테이블 적재로 확장 가능.
 */
@Component
class UserActionLogListener {

    companion object {
        private val log = LoggerFactory.getLogger(UserActionLogListener::class.java)
    }

    @Async(AsyncConfig.EVENT_ASYNC_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleUserAction(event: UserActionEvent) {
        try {
            log.info(
                "[유저 행동] action={}, userId={}, targetId={}, metadata={}",
                event.actionType,
                event.userId,
                event.targetId,
                event.metadata,
            )
        } catch (e: Exception) {
            log.warn("유저 행동 로깅 실패 [eventId={}]", event.eventId, e)
        }
    }
}
