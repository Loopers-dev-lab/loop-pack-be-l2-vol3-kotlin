package com.loopers.interfaces.api.admin.coupon

import com.loopers.application.admin.coupon.AdminCouponDeleteUseCase
import com.loopers.application.user.auth.UserAuthenticateUseCase
import com.loopers.application.user.queue.EntryTokenValidationUseCase
import com.loopers.application.admin.coupon.AdminCouponDetailUseCase
import com.loopers.application.admin.coupon.AdminCouponIssueListUseCase
import com.loopers.application.admin.coupon.AdminCouponListUseCase
import com.loopers.application.admin.coupon.AdminCouponRegisterUseCase
import com.loopers.application.admin.coupon.AdminCouponResult
import com.loopers.application.admin.coupon.AdminCouponUpdateUseCase
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.hamcrest.Matchers.nullValue
import java.math.BigDecimal
import java.time.ZonedDateTime

@DisplayName("Admin Coupon API contract")
@WebMvcTest(AdminCouponV1Controller::class)
class AdminCouponV1ControllerTest
@Autowired
constructor(
    private val mockMvc: MockMvc,
    @MockitoBean private val registerUseCase: AdminCouponRegisterUseCase,
    @MockitoBean private val updateUseCase: AdminCouponUpdateUseCase,
    @MockitoBean private val deleteUseCase: AdminCouponDeleteUseCase,
    @MockitoBean private val detailUseCase: AdminCouponDetailUseCase,
    @MockitoBean private val listUseCase: AdminCouponListUseCase,
    @MockitoBean private val issueListUseCase: AdminCouponIssueListUseCase,
    @MockitoBean private val userAuthenticateUseCase: UserAuthenticateUseCase,
    @MockitoBean private val entryTokenValidationUseCase: EntryTokenValidationUseCase,
) {
    companion object {
        private const val ENDPOINT = "/api-admin/v1/coupons"
        private const val LDAP = "loopers.admin"
        private val EXPIRED_AT = ZonedDateTime.parse("2099-12-31T23:59:59+09:00")
    }

    @Nested
    @DisplayName("POST /api-admin/v1/coupons")
    inner class Register {
        @Test
        @DisplayName("issueLimit 포함 요청은 201 Created와 issueLimit을 반환한다")
        fun register_withIssueLimit_returns201() {
            given(registerUseCase.register(any())).willReturn(
                AdminCouponResult.Register(
                    id = 1L,
                    name = "테스트 쿠폰",
                    type = "FIXED",
                    discountValue = 1000L,
                    minOrderAmount = null,
                    expiredAt = EXPIRED_AT,
                    issueLimit = 100L,
                ),
            )

            val body = """
                {
                    "name": "테스트 쿠폰",
                    "type": "FIXED",
                    "discountValue": 1000,
                    "issueLimit": 100,
                    "expiredAt": "2099-12-31T23:59:59+09:00"
                }
            """.trimIndent()

            mockMvc.perform(
                post(ENDPOINT)
                    .header("X-Loopers-Ldap", LDAP)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.issueLimit").value(100L))
        }
    }

    @Nested
    @DisplayName("PUT /api-admin/v1/coupons/{couponId}")
    inner class Update {
        @Test
        @DisplayName("issueLimit 포함 요청은 200 OK와 issueLimit을 반환한다")
        fun update_withIssueLimit_returns200() {
            given(updateUseCase.update(any())).willReturn(
                AdminCouponResult.Update(
                    id = 1L,
                    name = "수정된 쿠폰",
                    type = "RATE",
                    discountValue = 2000L,
                    minOrderAmount = BigDecimal("10000"),
                    expiredAt = EXPIRED_AT,
                    issueLimit = null,
                ),
            )

            val body = """
                {
                    "name": "수정된 쿠폰",
                    "discountValue": 2000,
                    "minOrderAmount": 10000,
                    "issueLimit": null,
                    "expiredAt": "2099-12-31T23:59:59+09:00"
                }
            """.trimIndent()

            mockMvc.perform(
                put("$ENDPOINT/1")
                    .header("X-Loopers-Ldap", LDAP)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.issueLimit").value(nullValue()))
        }
    }
}
