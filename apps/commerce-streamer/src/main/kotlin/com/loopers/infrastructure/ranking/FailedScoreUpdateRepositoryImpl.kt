package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.model.FailedScoreUpdate
import com.loopers.domain.ranking.repository.FailedScoreUpdateRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface FailedScoreUpdateJpaRepository : JpaRepository<FailedScoreUpdateEntity, Long> {
    fun findByRetryCountLessThan(maxRetryCount: Int, pageable: PageRequest): List<FailedScoreUpdateEntity>
}

@Repository
class FailedScoreUpdateRepositoryImpl(
    private val jpaRepository: FailedScoreUpdateJpaRepository,
) : FailedScoreUpdateRepository {

    override fun save(failedScoreUpdate: FailedScoreUpdate): FailedScoreUpdate {
        return jpaRepository.save(FailedScoreUpdateEntity.fromDomain(failedScoreUpdate)).toDomain()
    }

    override fun findPendingUpdates(maxRetryCount: Int, limit: Int): List<FailedScoreUpdate> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")),
        )
        return jpaRepository.findByRetryCountLessThan(maxRetryCount, pageable)
            .map { it.toDomain() }
    }

    override fun delete(failedScoreUpdate: FailedScoreUpdate) {
        jpaRepository.deleteById(failedScoreUpdate.id)
    }

    override fun deleteById(id: Long) {
        jpaRepository.deleteById(id)
    }
}
