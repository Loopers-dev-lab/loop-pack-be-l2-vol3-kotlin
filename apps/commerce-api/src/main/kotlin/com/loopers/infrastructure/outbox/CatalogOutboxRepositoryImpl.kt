package com.loopers.infrastructure.outbox

import com.loopers.domain.outbox.model.CatalogOutbox
import com.loopers.domain.outbox.repository.CatalogOutboxRepository
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface CatalogOutboxJpaRepository : JpaRepository<CatalogOutboxEntity, Long> {
    fun findAllByPublishedFalseOrderByIdAsc(limit: Limit): List<CatalogOutboxEntity>
}

@Repository
class CatalogOutboxRepositoryImpl(
    private val catalogOutboxJpaRepository: CatalogOutboxJpaRepository,
) : CatalogOutboxRepository {

    override fun save(outbox: CatalogOutbox): CatalogOutbox {
        return catalogOutboxJpaRepository.save(CatalogOutboxEntity.fromDomain(outbox)).toDomain()
    }

    override fun findAllUnpublished(limit: Int): List<CatalogOutbox> {
        return catalogOutboxJpaRepository
            .findAllByPublishedFalseOrderByIdAsc(Limit.of(limit))
            .map { it.toDomain() }
    }
}
