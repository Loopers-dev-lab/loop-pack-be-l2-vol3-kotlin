package com.loopers.interfaces.api.order

import com.loopers.application.auth.AuthFacade
import com.loopers.application.order.OrderFacade
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/orders")
class OrderV1Controller(
    private val authFacade: AuthFacade,
    private val orderFacade: OrderFacade,
) : OrderV1ApiSpec {

    @PostMapping
    override fun createOrder(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @RequestBody request: OrderV1Dto.CreateRequest,
    ): ApiResponse<OrderV1Dto.DetailResponse> {
        val member = authFacade.authenticate(loginId, password)

        return orderFacade.createOrder(member.id!!, request.toCommand())
            .let { OrderV1Dto.DetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{id}")
    override fun getById(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable id: Long,
    ): ApiResponse<OrderV1Dto.DetailResponse> {
        val member = authFacade.authenticate(loginId, password)

        return orderFacade.getById(id, member.id!!)
            .let { OrderV1Dto.DetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/me")
    override fun getMyOrders(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
    ): ApiResponse<List<OrderV1Dto.MainResponse>> {
        val member = authFacade.authenticate(loginId, password)

        return orderFacade.getMyOrders(member.id!!)
            .map { OrderV1Dto.MainResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PatchMapping("/{id}/cancel")
    override fun cancel(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable id: Long,
    ): ApiResponse<Any> {
        val member = authFacade.authenticate(loginId, password)

        orderFacade.cancel(id, member.id!!)
        return ApiResponse.success()
    }
}
