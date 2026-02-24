package com.loopers.infrastructure.point

import com.loopers.domain.point.model.PointHistory
import com.loopers.domain.point.repository.PointHistoryRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface PointHistoryJpaRepository : JpaRepository<PointHistoryEntity, Long> {
    fun findAllByRefUserPointId(refUserPointId: Long): List<PointHistoryEntity>
}

@Repository
class PointHistoryRepositoryImpl(
    private val jpa: PointHistoryJpaRepository,
) : PointHistoryRepository {

    override fun save(pointHistory: PointHistory): PointHistory {
        return jpa.save(PointHistoryEntity.fromDomain(pointHistory)).toDomain()
    }

    override fun findAllByUserPointId(userPointId: Long): List<PointHistory> {
        return jpa.findAllByRefUserPointId(userPointId).map { it.toDomain() }
    }
}
