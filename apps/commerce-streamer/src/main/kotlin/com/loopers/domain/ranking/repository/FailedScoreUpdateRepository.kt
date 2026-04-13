package com.loopers.domain.ranking.repository

import com.loopers.domain.ranking.model.FailedScoreUpdate

interface FailedScoreUpdateRepository {
    fun save(failedScoreUpdate: FailedScoreUpdate): FailedScoreUpdate
    fun findPendingUpdates(maxRetryCount: Int, limit: Int): List<FailedScoreUpdate>
    fun delete(failedScoreUpdate: FailedScoreUpdate)
    fun deleteById(id: Long)
}
