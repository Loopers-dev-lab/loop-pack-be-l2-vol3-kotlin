package com.loopers.domain.useractionlog

import java.time.LocalDate

data class UserActionLogAppendCommand(
    val actionType: String,
    val actorUserId: Long,
    val targetId: String,
    val payload: String,
    val dedupeKey: String,
    val partitionDate: LocalDate,
)
