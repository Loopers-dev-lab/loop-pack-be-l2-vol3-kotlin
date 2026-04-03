package com.loopers.application.user.queue

class QueueCommand {
    data class Enter(val userId: Long)

    data class Position(val userId: Long)
}
