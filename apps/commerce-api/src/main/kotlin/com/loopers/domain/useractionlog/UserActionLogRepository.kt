package com.loopers.domain.useractionlog

interface UserActionLogRepository {
    fun save(userActionLog: UserActionLog): UserActionLog

    fun existsByDedupeKey(dedupeKey: String): Boolean

    fun appendIfAbsent(command: UserActionLogAppendCommand): Boolean
}
