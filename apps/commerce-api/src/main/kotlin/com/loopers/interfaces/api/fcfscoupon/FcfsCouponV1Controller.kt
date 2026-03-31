package com.loopers.interfaces.api.fcfscoupon

import com.loopers.application.fcfscoupon.FcfsCouponFacade
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.config.auth.AuthenticatedMember
import com.loopers.interfaces.config.auth.MemberAuthenticated
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@MemberAuthenticated
@RestController
@RequestMapping("/api/v1/fcfs-coupons")
class FcfsCouponV1Controller(
    private val fcfsCouponFacade: FcfsCouponFacade,
) {
    @PostMapping("/templates/{templateId}/issue")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun requestIssue(
        authenticatedMember: AuthenticatedMember,
        @PathVariable templateId: Long,
    ): ApiResponse<FcfsCouponV1Dto.IssueRequestResponse> {
        return fcfsCouponFacade.requestIssue(authenticatedMember.id, templateId)
            .let { FcfsCouponV1Dto.IssueRequestResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/requests/{requestId}")
    fun getIssueRequestStatus(
        authenticatedMember: AuthenticatedMember,
        @PathVariable requestId: Long,
    ): ApiResponse<FcfsCouponV1Dto.IssueRequestResponse> {
        return fcfsCouponFacade.getIssueRequestStatus(requestId, authenticatedMember.id)
            .let { FcfsCouponV1Dto.IssueRequestResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
