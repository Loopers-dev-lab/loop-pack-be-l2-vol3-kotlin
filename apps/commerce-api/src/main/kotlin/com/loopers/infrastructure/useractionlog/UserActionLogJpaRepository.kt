package com.loopers.infrastructure.useractionlog

import com.loopers.domain.useractionlog.UserActionLog
import org.springframework.data.jpa.repository.JpaRepository

interface UserActionLogJpaRepository : JpaRepository<UserActionLog, Long> {
    fun existsByDedupeKey(dedupeKey: String): Boolean

    fun countByDedupeKey(dedupeKey: String): Long

    fun countByActionType(actionType: String): Long
}
