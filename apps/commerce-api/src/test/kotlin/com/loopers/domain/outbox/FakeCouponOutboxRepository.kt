package com.loopers.domain.outbox

import com.loopers.domain.outbox.model.CouponOutbox
import com.loopers.domain.outbox.repository.CouponOutboxRepository

class FakeCouponOutboxRepository : CouponOutboxRepository {

    private val store = mutableListOf<CouponOutbox>()
    private var sequence = 1L

    override fun save(outbox: CouponOutbox): CouponOutbox {
        if (outbox.id != 0L) {
            store.removeIf { it.id == outbox.id }
            store.add(outbox)
        } else {
            setId(outbox, sequence++)
            store.add(outbox)
        }
        return outbox
    }

    override fun findAllUnpublished(): List<CouponOutbox> {
        return store.filter { !it.published }
    }

    private fun setId(entity: CouponOutbox, id: Long) {
        CouponOutbox::class.java.getDeclaredField("id").apply {
            isAccessible = true
            set(entity, id)
        }
    }
}
