package com.loopers.domain.outbox.repository

import com.loopers.domain.outbox.model.CouponOutbox

interface CouponOutboxRepository {
    fun save(outbox: CouponOutbox): CouponOutbox
    fun findAllUnpublished(): List<CouponOutbox>
}
