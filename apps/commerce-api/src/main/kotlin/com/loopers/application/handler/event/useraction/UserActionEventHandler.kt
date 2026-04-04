package com.loopers.application.handler.event.useraction

import com.loopers.application.handler.command.useraction.PublishProductMetricsCommandHandler
import com.loopers.domain.common.command.PublishProductMetricsCommand
import com.loopers.domain.common.event.UserActionEvent
import com.loopers.domain.useraction.UserActionLogModel
import com.loopers.domain.useraction.UserActionLogRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class UserActionEventHandler(
    private val userActionLogRepository: UserActionLogRepository,
    private val publishProductMetricsCommandHandler: PublishProductMetricsCommandHandler,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: UserActionEvent) {
        userActionLogRepository.save(
            UserActionLogModel(
                memberId = event.memberId,
                actionType = event.actionType,
                targetType = event.targetType,
                targetId = event.targetId,
            ),
        )

        publishProductMetricsCommandHandler.handle(
            PublishProductMetricsCommand(
                memberId = event.memberId,
                actionType = event.actionType.name,
                targetType = event.targetType.name,
                targetId = event.targetId,
            ),
        )

        log.debug(
            "유저 행동 로그 저장 + 메트릭 발행: memberId={}, action={}, target={}:{}",
            event.memberId,
            event.actionType,
            event.targetType,
            event.targetId,
        )
    }
}
