package com.loopers.application.queue

import com.loopers.config.QueueProperties
import com.loopers.domain.queue.QueueEntryInfo
import com.loopers.domain.queue.QueuePositionInfo
import com.loopers.domain.queue.QueueService
import com.loopers.domain.user.UserService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class QueueFacade(
    private val queueService: QueueService,
    private val userService: UserService,
    private val queueProperties: QueueProperties,
) {
    fun enterQueue(loginId: String, password: String): QueueEntryInfo {
        if (!queueProperties.enabled) throw CoreException(ErrorType.QUEUE_NOT_ENABLED)
        val user = getAuthenticatedUser(loginId, password)
        return queueService.enterQueue(user.id)
            ?: throw CoreException(ErrorType.QUEUE_ALREADY_ENTERED)
    }

    fun getPosition(loginId: String, password: String): QueuePositionInfo {
        if (!queueProperties.enabled) throw CoreException(ErrorType.QUEUE_NOT_ENABLED)
        val user = getAuthenticatedUser(loginId, password)
        return queueService.getPosition(user.id)
    }

    private fun getAuthenticatedUser(loginId: String, password: String) =
        userService.getUserByLoginIdAndPassword(loginId, password)
            ?: throw CoreException(ErrorType.NOT_FOUND, "User not found")
}
