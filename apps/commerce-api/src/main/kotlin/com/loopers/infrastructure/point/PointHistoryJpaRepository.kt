package com.loopers.infrastructure.point

import com.loopers.domain.point.entity.PointHistory
import org.springframework.data.jpa.repository.JpaRepository

interface PointHistoryJpaRepository : JpaRepository<PointHistory, Long> {
    fun findAllByRefUserPointId(refUserPointId: Long): List<PointHistory>
}
