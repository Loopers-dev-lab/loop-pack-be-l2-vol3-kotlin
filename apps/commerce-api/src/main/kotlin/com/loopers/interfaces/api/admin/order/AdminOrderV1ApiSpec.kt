package com.loopers.interfaces.api.admin.order

import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import java.time.LocalDate

@Tag(name = "[Admin] Order V1 API", description = "[Admin] 주문 API 입니다.")
interface AdminOrderV1ApiSpec {

    @Operation(summary = "주문 목록 조회", description = "전체 주문 목록을 조회합니다.")
    fun getList(
        ldap: String,
        from: LocalDate?,
        to: LocalDate?,
        pageRequest: PageRequest,
    ): ApiResponse<PageResponse<AdminOrderV1Response.ListItem>>

    @Operation(summary = "주문 상세 조회", description = "주문 상세 정보를 조회합니다.")
    fun getDetail(
        ldap: String,
        orderId: Long,
    ): ApiResponse<AdminOrderV1Response.Detail>
}
