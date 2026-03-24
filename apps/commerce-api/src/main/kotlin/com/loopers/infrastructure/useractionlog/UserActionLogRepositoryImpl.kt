package com.loopers.infrastructure.useractionlog

import com.loopers.domain.useractionlog.UserActionLog
import com.loopers.domain.useractionlog.UserActionLogAppendCommand
import com.loopers.domain.useractionlog.UserActionLogRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository

@Repository
class UserActionLogRepositoryImpl(
    private val userActionLogJpaRepository: UserActionLogJpaRepository,
) : UserActionLogRepository {
    override fun save(userActionLog: UserActionLog): UserActionLog =
        userActionLogJpaRepository.save(userActionLog)

    override fun existsByDedupeKey(dedupeKey: String): Boolean =
        userActionLogJpaRepository.existsByDedupeKey(dedupeKey)

    override fun appendIfAbsent(command: UserActionLogAppendCommand): Boolean {
        if (existsByDedupeKey(command.dedupeKey)) {
            return false
        }

        return try {
            save(UserActionLog.append(command))
            true
        } catch (_: DataIntegrityViolationException) {
            false
        }
    }
}
