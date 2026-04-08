package com.loopers.infrastructure.queue

import org.springframework.stereotype.Component
import java.util.UUID

@Component
class QueueTokenGenerator {
    fun generate(): String {
        return UUID.randomUUID().toString()
    }
}
