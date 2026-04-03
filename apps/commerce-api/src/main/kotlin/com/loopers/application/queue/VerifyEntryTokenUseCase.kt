package com.loopers.application.queue

import com.loopers.domain.queue.OrderQueueStore
import org.springframework.stereotype.Component

@Component
class VerifyEntryTokenUseCase(
    private val orderQueueStore: OrderQueueStore,
) {

    fun execute(userId: Long): Boolean {
        return orderQueueStore.hasToken(userId)
    }
}
