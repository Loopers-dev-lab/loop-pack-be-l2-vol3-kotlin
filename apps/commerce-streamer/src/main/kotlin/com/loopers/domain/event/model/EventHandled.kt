package com.loopers.domain.event.model

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.ZonedDateTime

class EventHandled(
    val eventId: String,
    val handledAt: ZonedDateTime = ZonedDateTime.now(),
) {

    init {
        if (eventId.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "eventId는 필수입니다.")
    }
}
