package com.loopers.interfaces.api.order

import com.loopers.domain.user.User
import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Order API", description = "주문 API")
interface OrderApiSpec {

    @Operation(
        summary = "주문 요청",
        description = "상품을 주문합니다. 재고가 차감되고 주문이 생성됩니다.",
    )
    fun placeOrder(user: User, request: OrderDto.PlaceOrderRequest): ApiResponse<Unit>
}
