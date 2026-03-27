package com.loopers.application.fcfscoupon

import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import com.loopers.domain.fcfscoupon.FcfsCouponTemplateModel
import com.loopers.domain.fcfscoupon.FcfsCouponTemplateRepository
import java.time.ZonedDateTime

class FakeFcfsCouponTemplateRepository : FcfsCouponTemplateRepository {
    private val store = mutableMapOf<Long, FcfsCouponTemplateModel>()
    private var idSequence = 1L

    override fun save(template: FcfsCouponTemplateModel): FcfsCouponTemplateModel {
        val saved = if (template.id == 0L) {
            template.copy(id = idSequence++, createdAt = ZonedDateTime.now(), updatedAt = ZonedDateTime.now())
        } else {
            template.copy(updatedAt = ZonedDateTime.now())
        }
        store[saved.id] = saved
        return saved
    }

    override fun findById(id: Long): FcfsCouponTemplateModel? = store[id]

    override fun findAll(pageQuery: PageQuery): PageResult<FcfsCouponTemplateModel> {
        val all = store.values.sortedByDescending { it.id }
        val start = pageQuery.page * pageQuery.size
        val content = all.drop(start).take(pageQuery.size)
        return PageResult(content, all.size.toLong(), (all.size + pageQuery.size - 1) / pageQuery.size)
    }

    fun clear() {
        store.clear()
        idSequence = 1L
    }
}
