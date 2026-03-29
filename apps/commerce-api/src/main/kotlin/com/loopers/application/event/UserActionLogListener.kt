package com.loopers.application.event

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class UserActionLogListener {
    private val log = LoggerFactory.getLogger(UserActionLogListener::class.java)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: UserActionLogEvent) {
        log.info(
            "user-action actionType={} memberId={} targetType={} targetId={} details={} occurredAt={}",
            event.actionType,
            event.memberId,
            event.targetType,
            event.targetId,
            event.details,
            event.occurredAt,
        )
    }
}
