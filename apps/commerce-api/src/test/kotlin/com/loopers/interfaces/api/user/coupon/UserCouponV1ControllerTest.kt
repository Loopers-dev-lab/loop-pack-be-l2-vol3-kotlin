package com.loopers.interfaces.api.user.coupon

import com.loopers.application.user.auth.UserAuthenticateUseCase
import com.loopers.application.user.coupon.UserCouponIssueRequestStatusUseCase
import com.loopers.application.user.coupon.UserCouponIssueUseCase
import com.loopers.application.user.coupon.UserCouponListUseCase
import com.loopers.application.user.coupon.UserCouponResult
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.hamcrest.Matchers.nullValue

@DisplayName("User Coupon API contract")
@WebMvcTest(UserCouponV1Controller::class)
class UserCouponV1ControllerTest
@Autowired
constructor(
    private val mockMvc: MockMvc,
    @MockitoBean private val userAuthenticateUseCase: UserAuthenticateUseCase,
    @MockitoBean private val issueUseCase: UserCouponIssueUseCase,
    @MockitoBean private val issueRequestStatusUseCase: UserCouponIssueRequestStatusUseCase,
    @MockitoBean private val userCouponListUseCase: UserCouponListUseCase,
) {
    companion object {
        private const val LOGIN_ID = "testuser1"
        private const val PASSWORD = "Password1!"
        private const val COUPON_ID = 11L
        private const val REQUEST_ID = 101L
        private const val ENDPOINT = "/api/v1/coupons/$COUPON_ID/issue"
    }

    @Nested
    @DisplayName("POST /api/v1/coupons/{couponId}/issue")
    inner class Issue {
        @Test
        @DisplayName("유효한 요청이면 202 Accepted와 접수 결과를 반환한다")
        fun issue_success_returns202() {
            given(userAuthenticateUseCase.authenticateAndGetId(LOGIN_ID, PASSWORD)).willReturn(1L)
            given(issueUseCase.issue(com.loopers.application.user.coupon.UserCouponCommand.Issue(1L, COUPON_ID)))
                .willReturn(
                    UserCouponResult.IssueRequest(
                        requestId = REQUEST_ID,
                        couponId = COUPON_ID,
                        status = "REQUESTED",
                    ),
                )

            mockMvc.perform(
                post(ENDPOINT)
                    .header("X-Loopers-LoginId", LOGIN_ID)
                    .header("X-Loopers-LoginPw", PASSWORD),
            )
                .andExpect(status().isAccepted)
                .andExpect(jsonPath("$.data.requestId").value(REQUEST_ID))
                .andExpect(jsonPath("$.data.couponId").value(COUPON_ID))
                .andExpect(jsonPath("$.data.status").value("REQUESTED"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/me/coupon-issue-requests/{requestId}")
    inner class GetIssueRequestStatus {
        @Test
        @DisplayName("내 요청이면 200 OK와 상태 상세를 반환한다")
        fun getIssueRequestStatus_success_returns200() {
            given(userAuthenticateUseCase.authenticateAndGetId(LOGIN_ID, PASSWORD)).willReturn(1L)
            given(
                issueRequestStatusUseCase.getStatus(
                    com.loopers.application.user.coupon.UserCouponCommand.IssueRequestStatus(
                        userId = 1L,
                        requestId = REQUEST_ID,
                    ),
                ),
            ).willReturn(
                UserCouponResult.IssueRequestStatus(
                    requestId = REQUEST_ID,
                    couponId = COUPON_ID,
                    status = "ISSUED",
                    failureReasonCode = null,
                    issuedCouponId = 555L,
                ),
            )

            mockMvc.perform(
                get("/api/v1/users/me/coupon-issue-requests/$REQUEST_ID")
                    .header("X-Loopers-LoginId", LOGIN_ID)
                    .header("X-Loopers-LoginPw", PASSWORD),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.requestId").value(REQUEST_ID))
                .andExpect(jsonPath("$.data.couponId").value(COUPON_ID))
                .andExpect(jsonPath("$.data.status").value("ISSUED"))
                .andExpect(jsonPath("$.data.failureReasonCode").value(nullValue()))
                .andExpect(jsonPath("$.data.issuedCouponId").value(555L))
        }

        @Test
        @DisplayName("다른 사용자의 요청이면 404 Not Found를 반환한다")
        fun getIssueRequestStatus_otherUser_returns404() {
            given(userAuthenticateUseCase.authenticateAndGetId(LOGIN_ID, PASSWORD)).willReturn(2L)
            given(
                issueRequestStatusUseCase.getStatus(
                    com.loopers.application.user.coupon.UserCouponCommand.IssueRequestStatus(
                        userId = 2L,
                        requestId = REQUEST_ID,
                    ),
                ),
            ).willThrow(CoreException(ErrorType.COUPON_ISSUE_REQUEST_NOT_FOUND))

            mockMvc.perform(
                get("/api/v1/users/me/coupon-issue-requests/$REQUEST_ID")
                    .header("X-Loopers-LoginId", LOGIN_ID)
                    .header("X-Loopers-LoginPw", PASSWORD),
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.meta.errorCode").value("COUPON_ISSUE_REQUEST_NOT_FOUND"))
        }
    }
}
