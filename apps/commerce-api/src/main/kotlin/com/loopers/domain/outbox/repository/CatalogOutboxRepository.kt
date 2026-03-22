package com.loopers.domain.outbox.repository

import com.loopers.domain.outbox.model.CatalogOutbox

interface CatalogOutboxRepository {
    fun save(outbox: CatalogOutbox): CatalogOutbox
    fun findAllUnpublished(): List<CatalogOutbox>
}
