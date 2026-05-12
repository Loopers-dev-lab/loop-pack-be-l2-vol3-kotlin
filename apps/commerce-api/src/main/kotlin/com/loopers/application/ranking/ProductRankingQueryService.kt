package com.loopers.application.ranking

import com.loopers.domain.ranking.RankingCheckpointScope
import com.loopers.domain.ranking.RankingFinalizedScope
import com.loopers.domain.ranking.RankingTargetType
import com.loopers.infrastructure.ranking.ProductRankMonthlyMvJpaRepository
import com.loopers.infrastructure.ranking.ProductRankWeeklyMvJpaRepository
import com.loopers.infrastructure.ranking.ProductRankingRedisRepository
import com.loopers.infrastructure.ranking.RankingCheckpointSnapshotJpaRepository
import com.loopers.infrastructure.ranking.RankingFinalizedSnapshotJpaRepository
import com.loopers.infrastructure.ranking.RankingRedisKeys
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime

@Service
class ProductRankingQueryService(
    private val productRankingRedisRepository: ProductRankingRedisRepository,
    private val rankingFinalizedSnapshotJpaRepository: RankingFinalizedSnapshotJpaRepository,
    private val rankingCheckpointSnapshotJpaRepository: RankingCheckpointSnapshotJpaRepository,
    private val productRankWeeklyMvJpaRepository: ProductRankWeeklyMvJpaRepository,
    private val productRankMonthlyMvJpaRepository: ProductRankMonthlyMvJpaRepository,
) {
    companion object {
        private const val SEGMENT_KEY = RankingRedisKeys.SEGMENT
        private val TARGET_TYPE = RankingTargetType.PRODUCT
        private val RANK_SORT: Sort = Sort.by(Sort.Direction.ASC, "rankPosition")
    }

    data class RankingEntry(
        val productId: Long,
        val rank: Long,
        val score: Double,
    )

    data class RankingPageResult(
        val items: List<RankingEntry>,
        val page: Int,
        val size: Int,
        val totalCount: Long,
    ) {
        val totalPages: Int
            get() = if (totalCount == 0L) 0 else ((totalCount + size - 1) / size).toInt()
    }

    fun getRankingPage(
        type: RankingType,
        page: Int,
        size: Int,
        date: LocalDate?,
        weekStartDate: LocalDate?,
        yearMonth: YearMonth?,
        now: ZonedDateTime = ZonedDateTime.now(RankingRedisKeys.ZONE_ID),
    ): RankingPageResult {
        validate(page, size)
        return when (type) {
            RankingType.REALTIME -> getRollingPage(RankingCheckpointScope.REALTIME, 60, page, size, now)
            RankingType.DAILY -> getRollingPage(RankingCheckpointScope.DAILY, 1_440, page, size, now)
            RankingType.WEEKLY -> getFinalizedPage(RankingFinalizedScope.WEEKLY, latestAsOfDate(RankingFinalizedScope.WEEKLY), page, size)
            RankingType.MONTHLY -> getFinalizedPage(RankingFinalizedScope.MONTHLY, latestAsOfDate(RankingFinalizedScope.MONTHLY), page, size)
            RankingType.DAY_FIXED -> getFinalizedPage(
                scope = RankingFinalizedScope.DAY,
                asOfDate = date ?: throw CoreException(ErrorType.BAD_REQUEST, "date 파라미터가 필요합니다."),
                page = page,
                size = size,
            )
            RankingType.WEEK_FIXED -> getWeekFixedPage(weekStartDate, page, size)
            RankingType.MONTH_FIXED -> getMonthFixedPage(yearMonth, page, size)
        }
    }

    fun getWeeklyRank(productId: Long): Long? =
        findLatestFinalizedRank(RankingFinalizedScope.WEEKLY, productId)

    fun getMonthlyRank(productId: Long): Long? =
        findLatestFinalizedRank(RankingFinalizedScope.MONTHLY, productId)

    private fun getRollingPage(
        scope: RankingCheckpointScope,
        minutes: Long,
        page: Int,
        size: Int,
        now: ZonedDateTime,
    ): RankingPageResult =
        try {
            productRankingRedisRepository.getPageFromRollingKeys(
                keys = RankingRedisKeys.rollingMinuteKeys(now, minutes),
                page = page,
                size = size,
            ).let { result ->
                RankingPageResult(
                    items = result.content.map { RankingEntry(it.targetId, it.rank, it.score) },
                    page = page,
                    size = size,
                    totalCount = result.totalCount,
                )
            }
        } catch (_: Exception) {
            val capturedAt = rankingCheckpointSnapshotJpaRepository.findLatestCapturedAt(TARGET_TYPE, SEGMENT_KEY, scope)
                ?: return RankingPageResult(emptyList(), page, size, 0)
            val pageable = PageRequest.of(page - 1, size, RANK_SORT)
            val fallback = rankingCheckpointSnapshotJpaRepository.findAllByTargetTypeAndSegmentKeyAndScopeAndCapturedAt(
                targetType = TARGET_TYPE,
                segmentKey = SEGMENT_KEY,
                scope = scope,
                capturedAt = capturedAt,
                pageable = pageable,
            )
            RankingPageResult(
                items = fallback.content.map { RankingEntry(it.targetId, it.rankPosition, it.score) },
                page = page,
                size = size,
                totalCount = fallback.totalElements,
            )
        }

    private fun getFinalizedPage(
        scope: RankingFinalizedScope,
        asOfDate: LocalDate?,
        page: Int,
        size: Int,
    ): RankingPageResult {
        if (asOfDate == null) {
            return RankingPageResult(emptyList(), page, size, 0)
        }
        val key = when (scope) {
            RankingFinalizedScope.DAY -> RankingRedisKeys.dayBucket(asOfDate)
            RankingFinalizedScope.WEEKLY -> RankingRedisKeys.weeklyView(asOfDate)
            RankingFinalizedScope.MONTHLY -> RankingRedisKeys.monthlyView(asOfDate)
        }
        runCatching {
            if (productRankingRedisRepository.hasKey(key)) {
                return productRankingRedisRepository.getPageFromKey(key, page, size)
                    .let { result ->
                        RankingPageResult(
                            items = result.content.map { RankingEntry(it.targetId, it.rank, it.score) },
                            page = page,
                            size = size,
                            totalCount = result.totalCount,
                        )
                    }
            }
        }

        val pageable = PageRequest.of(page - 1, size, RANK_SORT)
        val fallback = rankingFinalizedSnapshotJpaRepository.findAllByTargetTypeAndSegmentKeyAndScopeAndAsOfDate(
            targetType = TARGET_TYPE,
            segmentKey = SEGMENT_KEY,
            scope = scope,
            asOfDate = asOfDate,
            pageable = pageable,
        )
        return RankingPageResult(
            items = fallback.content.map { RankingEntry(it.targetId, it.rankPosition, it.score) },
            page = page,
            size = size,
            totalCount = fallback.totalElements,
        )
    }

    private fun findLatestFinalizedRank(
        scope: RankingFinalizedScope,
        productId: Long,
    ): Long? {
        val asOfDate = rankingFinalizedSnapshotJpaRepository.findLatestAsOfDate(TARGET_TYPE, SEGMENT_KEY, scope)
            ?: return null
        val key = when (scope) {
            RankingFinalizedScope.DAY -> RankingRedisKeys.dayBucket(asOfDate)
            RankingFinalizedScope.WEEKLY -> RankingRedisKeys.weeklyView(asOfDate)
            RankingFinalizedScope.MONTHLY -> RankingRedisKeys.monthlyView(asOfDate)
        }

        return runCatching {
            if (productRankingRedisRepository.hasKey(key)) {
                productRankingRedisRepository.findRankFromKey(key, productId)
            } else {
                null
            }
        }.getOrNull() ?: rankingFinalizedSnapshotJpaRepository.findByTargetTypeAndSegmentKeyAndScopeAndAsOfDateAndTargetId(
            targetType = TARGET_TYPE,
            segmentKey = SEGMENT_KEY,
            scope = scope,
            asOfDate = asOfDate,
            targetId = productId,
        )?.rankPosition
    }

    private fun latestAsOfDate(scope: RankingFinalizedScope): LocalDate? =
        rankingFinalizedSnapshotJpaRepository.findLatestAsOfDate(TARGET_TYPE, SEGMENT_KEY, scope)

    private fun getWeekFixedPage(
        weekStartDate: LocalDate?,
        page: Int,
        size: Int,
    ): RankingPageResult {
        val periodStartDate = weekStartDate ?: productRankWeeklyMvJpaRepository.findLatestPeriodStartDate()
            ?: return RankingPageResult(emptyList(), page, size, 0)

        val pageable = PageRequest.of(page - 1, size, RANK_SORT)
        val snapshots = productRankWeeklyMvJpaRepository.findAllByPeriodStartDate(periodStartDate, pageable)
        return RankingPageResult(
            items = snapshots.content.map { RankingEntry(it.productId, it.rankPosition, it.score) },
            page = page,
            size = size,
            totalCount = snapshots.totalElements,
        )
    }

    private fun getMonthFixedPage(
        yearMonth: YearMonth?,
        page: Int,
        size: Int,
    ): RankingPageResult {
        val periodStartDate = yearMonth?.atDay(1) ?: productRankMonthlyMvJpaRepository.findLatestPeriodStartDate()
            ?: return RankingPageResult(emptyList(), page, size, 0)

        val pageable = PageRequest.of(page - 1, size, RANK_SORT)
        val snapshots = productRankMonthlyMvJpaRepository.findAllByPeriodStartDate(periodStartDate, pageable)
        return RankingPageResult(
            items = snapshots.content.map { RankingEntry(it.productId, it.rankPosition, it.score) },
            page = page,
            size = size,
            totalCount = snapshots.totalElements,
        )
    }

    private fun validate(page: Int, size: Int) {
        if (page < 1 || size < 1) {
            throw CoreException(ErrorType.BAD_REQUEST, "page와 size는 1 이상이어야 합니다.")
        }
    }
}
