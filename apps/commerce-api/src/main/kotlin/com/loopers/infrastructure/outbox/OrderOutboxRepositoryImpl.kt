package com.loopers.infrastructure.outbox

import com.loopers.domain.outbox.model.OrderOutbox
import com.loopers.domain.outbox.repository.OrderOutboxRepository
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface OrderOutboxJpaRepository : JpaRepository<OrderOutboxEntity, Long> {
    fun findAllByPublishedFalseOrderByIdAsc(limit: Limit): List<OrderOutboxEntity>
}

@Repository
class OrderOutboxRepositoryImpl(
    private val orderOutboxJpaRepository: OrderOutboxJpaRepository,
) : OrderOutboxRepository {

    override fun save(outbox: OrderOutbox): OrderOutbox {
        return orderOutboxJpaRepository.save(OrderOutboxEntity.fromDomain(outbox)).toDomain()
    }

    override fun findAllUnpublished(limit: Int): List<OrderOutbox> {
        return orderOutboxJpaRepository
            .findAllByPublishedFalseOrderByIdAsc(Limit.of(limit))
            .map { it.toDomain() }
    }
}
