package com.loopers.domain.outbox

import com.loopers.domain.outbox.model.CatalogOutbox
import com.loopers.domain.outbox.repository.CatalogOutboxRepository

class FakeCatalogOutboxRepository : CatalogOutboxRepository {

    private val store = mutableListOf<CatalogOutbox>()
    private var sequence = 1L

    override fun save(outbox: CatalogOutbox): CatalogOutbox {
        if (outbox.id != 0L) {
            store.removeIf { it.id == outbox.id }
            store.add(outbox)
        } else {
            setId(outbox, sequence++)
            store.add(outbox)
        }
        return outbox
    }

    override fun findAllUnpublished(): List<CatalogOutbox> {
        return store.filter { !it.published }
    }

    private fun setId(entity: CatalogOutbox, id: Long) {
        CatalogOutbox::class.java.getDeclaredField("id").apply {
            isAccessible = true
            set(entity, id)
        }
    }
}
