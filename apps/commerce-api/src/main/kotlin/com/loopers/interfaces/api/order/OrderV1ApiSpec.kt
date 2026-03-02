package com.loopers.interfaces.api.order

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AuthenticatedUser
import com.loopers.interfaces.api.auth.RequireAuth
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

@Tag(name = "Order V1 API", description = "주문 API")
interface OrderV1ApiSpec {

    @Operation(
        summary = "주문 생성",
        description = "상품을 주문합니다.",
        parameters = [
            Parameter(
                name = "X-Loopers-LoginId",
                description = "사용자 로그인 ID",
                required = true,
                schema = Schema(type = "string"),
                `in` = ParameterIn.HEADER,
            ),
            Parameter(
                name = "X-Loopers-LoginPw",
                description = "사용자 비밀번호",
                required = true,
                schema = Schema(type = "string"),
                `in` = ParameterIn.HEADER,
            ),
        ],
    )
    fun createOrder(
        @Parameter(hidden = true) @RequireAuth authenticatedUser: AuthenticatedUser,
        @RequestBody request: OrderV1Dto.CreateOrderRequest,
    ): ApiResponse<OrderV1Dto.OrderResponse>

    @Operation(
        summary = "주문 목록 조회",
        description = "날짜 범위로 주문 목록을 조회합니다.",
        parameters = [
            Parameter(
                name = "X-Loopers-LoginId",
                description = "사용자 로그인 ID",
                required = true,
                schema = Schema(type = "string"),
                `in` = ParameterIn.HEADER,
            ),
            Parameter(
                name = "X-Loopers-LoginPw",
                description = "사용자 비밀번호",
                required = true,
                schema = Schema(type = "string"),
                `in` = ParameterIn.HEADER,
            ),
        ],
    )
    fun getOrders(
        @Parameter(hidden = true) @RequireAuth authenticatedUser: AuthenticatedUser,
        @RequestParam startAt: LocalDate,
        @RequestParam endAt: LocalDate,
        pageable: Pageable,
    ): ApiResponse<Page<OrderV1Dto.OrderResponse>>

    @Operation(
        summary = "주문 상세 조회",
        description = "주문 ID로 주문 상세 정보를 조회합니다.",
        parameters = [
            Parameter(
                name = "X-Loopers-LoginId",
                description = "사용자 로그인 ID",
                required = true,
                schema = Schema(type = "string"),
                `in` = ParameterIn.HEADER,
            ),
            Parameter(
                name = "X-Loopers-LoginPw",
                description = "사용자 비밀번호",
                required = true,
                schema = Schema(type = "string"),
                `in` = ParameterIn.HEADER,
            ),
        ],
    )
    fun getOrderById(
        @Parameter(hidden = true) @RequireAuth authenticatedUser: AuthenticatedUser,
        @PathVariable orderId: Long,
    ): ApiResponse<OrderV1Dto.OrderResponse>
}
