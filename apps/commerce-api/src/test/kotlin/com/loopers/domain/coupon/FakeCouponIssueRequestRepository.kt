package com.loopers.domain.coupon

import com.loopers.domain.coupon.model.CouponIssueRequest
import com.loopers.domain.coupon.repository.CouponIssueRequestRepository

class FakeCouponIssueRequestRepository : CouponIssueRequestRepository {

    private val store = mutableListOf<CouponIssueRequest>()
    private var sequence = 1L

    override fun save(request: CouponIssueRequest): CouponIssueRequest {
        if (request.id != 0L) {
            store.removeIf { it.id == request.id }
            store.add(request)
            return request
        }
        setId(request, sequence++)
        store.add(request)
        return request
    }

    override fun findByRequestId(requestId: String): CouponIssueRequest? {
        return store.find { it.requestId == requestId }
    }

    private fun setId(entity: CouponIssueRequest, id: Long) {
        CouponIssueRequest::class.java.getDeclaredField("id").apply {
            isAccessible = true
            set(entity, id)
        }
    }
}
