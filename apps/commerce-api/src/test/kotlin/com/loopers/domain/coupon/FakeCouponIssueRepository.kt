package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class FakeCouponIssueRepository : CouponIssueRepository {

    private val store = mutableListOf<CouponIssue>()
    private var idSequence = 1L

    override fun save(couponIssue: CouponIssue): CouponIssue {
        if (couponIssue.id == 0L) {
            setEntityId(couponIssue, idSequence++)
        }
        store.add(couponIssue)
        return couponIssue
    }

    override fun findById(id: Long): CouponIssue? {
        return store.find { it.id == id && it.deletedAt == null }
    }

    override fun findByUserIdAndCouponId(userId: Long, couponId: Long): CouponIssue? {
        return store.find {
            it.userId == userId && it.couponId == couponId && it.deletedAt == null
        }
    }

    override fun findAllByUserId(userId: Long): List<CouponIssue> {
        return store.filter { it.userId == userId && it.deletedAt == null }
    }

    override fun findAllByCouponId(couponId: Long, pageable: Pageable): Page<CouponIssue> {
        val filtered = store.filter { it.couponId == couponId && it.deletedAt == null }
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
