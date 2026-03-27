package com.loopers.application.fcfscoupon

import com.loopers.domain.fcfscoupon.FcfsCouponIssueRequestModel
import com.loopers.domain.fcfscoupon.FcfsCouponIssueRequestRepository
import java.time.ZonedDateTime

class FakeFcfsCouponIssueRequestRepository : FcfsCouponIssueRequestRepository {
    private val store = mutableMapOf<Long, FcfsCouponIssueRequestModel>()
    private var idSequence = 1L

    override fun save(request: FcfsCouponIssueRequestModel): FcfsCouponIssueRequestModel {
        val saved = if (request.id == 0L) {
            request.copy(id = idSequence++, createdAt = ZonedDateTime.now())
        } else {
            request
        }
        store[saved.id] = saved
        return saved
    }

    override fun findById(id: Long): FcfsCouponIssueRequestModel? = store[id]

    fun findAll(): List<FcfsCouponIssueRequestModel> = store.values.toList()

    fun clear() {
        store.clear()
        idSequence = 1L
    }
}
