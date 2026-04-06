package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity

class FakeCouponCompensationRepository : CouponCompensationRepository {

    private val store = mutableListOf<CouponCompensation>()
    private var idSequence = 1L

    override fun save(compensation: CouponCompensation): CouponCompensation {
        if (compensation.id == 0L) {
            setEntityId(compensation, idSequence++)
        }
        store.add(compensation)
        return compensation
    }

    override fun findAllByStatus(status: CompensationStatus): List<CouponCompensation> {
        return store.filter { it.status == status && it.deletedAt == null }
    }

    private fun setEntityId(entity: BaseEntity, id: Long) {
        val idField = BaseEntity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }
}
