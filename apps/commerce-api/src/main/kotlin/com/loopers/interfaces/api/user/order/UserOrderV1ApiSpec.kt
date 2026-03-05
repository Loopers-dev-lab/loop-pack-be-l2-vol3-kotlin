package com.loopers.interfaces.api.user.order

import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import java.time.LocalDate

@Tag(name = "[User] Order V1 API", description = "주문 API 입니다.")
interface UserOrderV1ApiSpec {

    @Operation(summary = "주문 생성", description = "새로운 주문을 생성합니다.")
    fun create(
        loginId: String,
        password: String,
        idempotencyKey: String,
        request: UserOrderV1Request.Create,
    ): ApiResponse<UserOrderV1Response.Created>

    @Operation(summary = "주문 목록 조회", description = "내 주문 목록을 조회합니다.")
    fun getList(
        loginId: String,
        password: String,
        from: LocalDate?,
        to: LocalDate?,
        pageRequest: PageRequest,
    ): ApiResponse<PageResponse<UserOrderV1Response.ListItem>>

    @Operation(summary = "주문 상세 조회", description = "주문 상세 정보를 조회합니다.")
    fun getDetail(
        loginId: String,
        password: String,
        orderId: Long,
    ): ApiResponse<UserOrderV1Response.Detail>
}
