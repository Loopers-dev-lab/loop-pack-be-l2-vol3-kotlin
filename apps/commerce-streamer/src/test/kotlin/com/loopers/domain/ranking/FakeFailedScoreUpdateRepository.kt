package com.loopers.domain.ranking

import com.loopers.domain.ranking.model.FailedScoreUpdate
import com.loopers.domain.ranking.repository.FailedScoreUpdateRepository

class FakeFailedScoreUpdateRepository : FailedScoreUpdateRepository {

    private val store = mutableListOf<FailedScoreUpdate>()
    private var idSequence = 1L

    override fun save(failedScoreUpdate: FailedScoreUpdate): FailedScoreUpdate {
        store.removeIf { it.id == failedScoreUpdate.id && failedScoreUpdate.id != 0L }
        val saved = FailedScoreUpdate(
            id = if (failedScoreUpdate.id == 0L) idSequence++ else failedScoreUpdate.id,
            eventId = failedScoreUpdate.eventId,
            productId = failedScoreUpdate.productId,
            score = failedScoreUpdate.score,
            rankingDate = failedScoreUpdate.rankingDate,
            createdAt = failedScoreUpdate.createdAt,
            retryCount = failedScoreUpdate.retryCount,
        )
        store.add(saved)
        return saved
    }

    override fun findPendingUpdates(maxRetryCount: Int, limit: Int): List<FailedScoreUpdate> {
        return store.filter { it.retryCount < maxRetryCount }
            .take(limit)
    }

    var deleteFailuresRemaining: Int = 0

    override fun delete(failedScoreUpdate: FailedScoreUpdate) {
        if (deleteFailuresRemaining > 0) {
            deleteFailuresRemaining--
            throw RuntimeException("DB delete 실패 (시뮬레이션)")
        }
        store.removeIf { it.id == failedScoreUpdate.id }
    }

    override fun deleteById(id: Long) {
        store.removeIf { it.id == id }
    }

    fun findAll(): List<FailedScoreUpdate> = store.toList()

    fun clear() {
        store.clear()
        idSequence = 1L
        deleteFailuresRemaining = 0
    }
}
