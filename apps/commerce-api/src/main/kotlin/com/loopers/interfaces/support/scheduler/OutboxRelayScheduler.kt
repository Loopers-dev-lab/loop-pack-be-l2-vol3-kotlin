package com.loopers.interfaces.support.scheduler

import com.loopers.application.event.RelayOutboxUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class OutboxRelayScheduler(
    private val relayOutboxUseCase: RelayOutboxUseCase,
) {

    @Scheduled(fixedDelay = 5000)
    fun relay() {
        relayOutboxUseCase.execute()
    }
}
