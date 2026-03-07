package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class FakeCouponRepository : CouponRepository {

    private val store = mutableListOf<Coupon>()
    private var idSequence = 1L

    override fun save(coupon: Coupon): Coupon {
        if (coupon.id == 0L) {
            setEntityId(coupon, idSequence++)
        }
        store.add(coupon)
        return coupon
    }

    override fun findById(id: Long): Coupon? {
        return store.find { it.id == id && it.deletedAt == null }
    }

    override fun findByIds(ids: List<Long>): List<Coupon> {
        return store.filter { it.id in ids && it.deletedAt == null }
    }

    override fun findAll(pageable: Pageable): Page<Coupon> {
        val filtered = store.filter { it.deletedAt == null }
        val start = pageable.offset.toInt()
        val end = minOf(start + pageable.pageSize, filtered.size)
        val content = if (start <= filtered.size) filtered.subList(start, end) else emptyList()
        return PageImpl(content, pageable, filtered.size.toLong())
    }

    private fun setEntityId(entity: BaseEntity, id: Long) {
        val idField = BaseEntity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }
}
