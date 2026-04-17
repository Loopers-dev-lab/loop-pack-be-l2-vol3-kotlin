package com.loopers.application.ranking

import com.loopers.common.DateUtils
import com.loopers.support.PageResult
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

@Component
class RankingSnapshotStore {

    private val snapshots = ConcurrentHashMap<String, PageResult<RankingProductInfo>>()

    fun save(date: LocalDate, rankings: PageResult<RankingProductInfo>) {
        snapshots[DateUtils.formatDate(date)] = rankings
        cleanupOldSnapshots()
    }

    fun get(date: LocalDate): PageResult<RankingProductInfo>? {
        return snapshots[DateUtils.formatDate(date)]
    }

    private fun cleanupOldSnapshots() {
        if (snapshots.size > MAX_SNAPSHOTS) {
            val sortedKeys = snapshots.keys().toList().sorted()
            val keysToRemove = sortedKeys.take(snapshots.size - MAX_SNAPSHOTS)
            keysToRemove.forEach { snapshots.remove(it) }
        }
    }

    companion object {
        private const val MAX_SNAPSHOTS = 3
    }
}
