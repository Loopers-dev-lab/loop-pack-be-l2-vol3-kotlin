package com.loopers.domain.eventhandled

import java.time.ZonedDateTime

data class EventHandledDto(
    val id: Long = 0,
    val dedupeKey: String,
    val createdAt: ZonedDateTime? = null,
)
