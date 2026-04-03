package com.loopers.infrastructure.queue

import com.loopers.application.queue.QueueEntryState
import com.loopers.application.queue.QueueExperimentProperties
import com.loopers.application.queue.QueueInfo
import com.loopers.application.queue.QueueStrategyType
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class QueueDbSupport(
    private val queueEntryJpaRepository: QueueEntryJpaRepository,
    private val queueExperimentProperties: QueueExperimentProperties,
    private val queueTokenGenerator: QueueTokenGenerator,
) {
    @Transactional
    fun enter(strategyType: QueueStrategyType, memberId: Long, queueOrder: Long): QueueInfo.Status {
        findActiveEntry(strategyType, memberId)?.let {
            return toStatus(it)
        }

        queueEntryJpaRepository.save(
            QueueEntryEntity(
                strategyType = strategyType,
                memberId = memberId,
                queueOrder = queueOrder,
                state = QueueEntryState.WAITING,
            ),
        )
        return getStatus(strategyType, memberId)
    }

    @Transactional
    fun getStatus(strategyType: QueueStrategyType, memberId: Long): QueueInfo.Status {
        val entry = findActiveEntry(strategyType, memberId)
            ?: return QueueInfo.Status(
                strategy = strategyType,
                state = QueueEntryState.NONE,
                position = null,
                totalWaitingCount = queueEntryJpaRepository.countByStrategyTypeAndState(strategyType, QueueEntryState.WAITING),
                expectedWaitSeconds = 0L,
                token = null,
                tokenExpiresAt = null,
            )

        if (entry.isExpired(ZonedDateTime.now())) {
            entry.expire()
            return QueueInfo.Status(
                strategy = strategyType,
                state = QueueEntryState.EXPIRED,
                position = null,
                totalWaitingCount = queueEntryJpaRepository.countByStrategyTypeAndState(strategyType, QueueEntryState.WAITING),
                expectedWaitSeconds = 0L,
                token = null,
                tokenExpiresAt = null,
            )
        }

        return toStatus(entry)
    }

    @Transactional
    fun admit(strategyType: QueueStrategyType, batchSize: Int): Int {
        val waitingEntries = queueEntryJpaRepository.findByStrategyTypeAndStateOrderByQueueOrderAsc(
            strategyType,
            QueueEntryState.WAITING,
            PageRequest.of(0, batchSize),
        )
        waitingEntries.forEach(::admitEntry)
        return waitingEntries.size
    }

    @Transactional
    fun admitWithPessimisticLock(strategyType: QueueStrategyType, batchSize: Int): Int {
        val waitingEntries = queueEntryJpaRepository.findWaitingForUpdate(
            strategyType.name,
            QueueEntryState.WAITING.name,
            batchSize,
        )
        waitingEntries.forEach(::admitEntry)
        return waitingEntries.size
    }

    @Transactional
    fun validateToken(strategyType: QueueStrategyType, memberId: Long, token: String) {
        val entry = queueEntryJpaRepository.findFirstByStrategyTypeAndToken(strategyType, token)
            ?: throw CoreException(ErrorType.INVALID_QUEUE_TOKEN)
        if (entry.memberId != memberId || entry.state != QueueEntryState.ADMITTED || entry.isExpired(ZonedDateTime.now())) {
            if (entry.isExpired(ZonedDateTime.now())) {
                entry.expire()
            }
            throw CoreException(ErrorType.INVALID_QUEUE_TOKEN)
        }
    }

    @Transactional
    fun complete(strategyType: QueueStrategyType, memberId: Long, token: String) {
        val entry = queueEntryJpaRepository.findFirstByStrategyTypeAndToken(strategyType, token) ?: return
        if (entry.memberId == memberId) {
            entry.complete()
        }
    }

    private fun findActiveEntry(strategyType: QueueStrategyType, memberId: Long): QueueEntryEntity? {
        return queueEntryJpaRepository.findFirstByStrategyTypeAndMemberIdAndStateInOrderByIdDesc(
            strategyType,
            memberId,
            listOf(QueueEntryState.WAITING, QueueEntryState.ADMITTED),
        )
    }

    private fun admitEntry(entry: QueueEntryEntity) {
        val token = queueTokenGenerator.generate()
        entry.admit(token, ZonedDateTime.now().plus(queueExperimentProperties.tokenTtl))
    }

    private fun toStatus(entry: QueueEntryEntity): QueueInfo.Status {
        val waitingCount = queueEntryJpaRepository.countByStrategyTypeAndState(entry.strategyType, QueueEntryState.WAITING)
        val position = if (entry.state == QueueEntryState.WAITING) {
            queueEntryJpaRepository.countAhead(entry.strategyType, QueueEntryState.WAITING, entry.queueOrder) + 1
        } else {
            0L
        }
        return QueueInfo.Status(
            strategy = entry.strategyType,
            state = entry.state,
            position = if (entry.state == QueueEntryState.WAITING) position else 0L,
            totalWaitingCount = waitingCount,
            expectedWaitSeconds = expectedWaitSeconds(position, entry.state),
            token = entry.token,
            tokenExpiresAt = entry.tokenExpiresAt,
        )
    }

    private fun expectedWaitSeconds(position: Long, state: QueueEntryState): Long {
        if (state != QueueEntryState.WAITING) {
            return 0L
        }
        val batchesAhead = (position - 1) / queueExperimentProperties.resolvedBatchSize() + 1
        return batchesAhead * queueExperimentProperties.scheduler.fixedDelay.seconds
    }
}
