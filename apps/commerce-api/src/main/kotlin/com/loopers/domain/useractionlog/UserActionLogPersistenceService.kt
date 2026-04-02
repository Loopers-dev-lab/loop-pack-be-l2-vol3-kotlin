package com.loopers.domain.useractionlog

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class UserActionLogPersistenceService(
    private val userActionLogRepository: UserActionLogRepository,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun appendIfAbsent(command: UserActionLogAppendCommand): Boolean =
        userActionLogRepository.appendIfAbsent(command)
}
