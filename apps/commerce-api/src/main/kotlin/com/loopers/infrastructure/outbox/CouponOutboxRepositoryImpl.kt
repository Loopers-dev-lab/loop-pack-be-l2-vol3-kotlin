package com.loopers.infrastructure.outbox

import com.loopers.domain.outbox.model.CouponOutbox
import com.loopers.domain.outbox.repository.CouponOutboxRepository
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface CouponOutboxJpaRepository : JpaRepository<CouponOutboxEntity, Long> {
    fun findAllByPublishedFalseOrderByIdAsc(limit: Limit): List<CouponOutboxEntity>
}

@Repository
class CouponOutboxRepositoryImpl(
    private val couponOutboxJpaRepository: CouponOutboxJpaRepository,
) : CouponOutboxRepository {

    override fun save(outbox: CouponOutbox): CouponOutbox {
        return couponOutboxJpaRepository.save(CouponOutboxEntity.fromDomain(outbox)).toDomain()
    }

    override fun findAllUnpublished(): List<CouponOutbox> {
        return couponOutboxJpaRepository
            .findAllByPublishedFalseOrderByIdAsc(Limit.of(100))
            .map { it.toDomain() }
    }
}
