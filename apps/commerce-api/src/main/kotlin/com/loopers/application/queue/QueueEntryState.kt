package com.loopers.application.queue

enum class QueueEntryState {
    WAITING,
    ADMITTED,
    COMPLETED,
    EXPIRED,
    NONE,
}
