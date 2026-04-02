package com.loopers.application.event

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class Slf4jUserActionLogWriter : UserActionLogWriter {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun write(command: UserActionLogCommand) {
        log.info(
            "user_action actionType={} userId={} targetId={} metadata={}",
            command.actionType,
            command.userId,
            command.targetId,
            command.metadata,
        )
    }
}
