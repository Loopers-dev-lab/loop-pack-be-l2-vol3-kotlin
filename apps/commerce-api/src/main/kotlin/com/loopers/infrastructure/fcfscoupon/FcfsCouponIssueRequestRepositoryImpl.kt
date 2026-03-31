package com.loopers.infrastructure.fcfscoupon

import com.loopers.domain.fcfscoupon.FcfsCouponIssueRequestModel
import com.loopers.domain.fcfscoupon.FcfsCouponIssueRequestRepository
import org.springframework.stereotype.Component

@Component
class FcfsCouponIssueRequestRepositoryImpl(
    private val jpaRepository: FcfsCouponIssueRequestJpaRepository,
) : FcfsCouponIssueRequestRepository {

    override fun save(request: FcfsCouponIssueRequestModel): FcfsCouponIssueRequestModel {
        return jpaRepository.save(FcfsCouponIssueRequestJpaModel.from(request)).toModel()
    }

    override fun findById(id: Long): FcfsCouponIssueRequestModel? {
        return jpaRepository.findById(id).orElse(null)?.toModel()
    }
}
