package com.loopers.interfaces.api.user.order

import com.loopers.application.user.auth.UserAuthenticateUseCase
import com.loopers.application.user.order.OrderCreateUseCase
import com.loopers.application.user.order.OrderDetailUseCase
import com.loopers.application.user.order.OrderListUseCase
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RequestMapping("/api/v1/orders")
@RestController
class UserOrderV1Controller(
    private val userAuthenticateUseCase: UserAuthenticateUseCase,
    private val createUseCase: OrderCreateUseCase,
    private val listUseCase: OrderListUseCase,
    private val detailUseCase: OrderDetailUseCase,
) : UserOrderV1ApiSpec {

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    override fun create(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @RequestHeader("X-Idempotency-Key") idempotencyKey: String,
        @Valid @RequestBody request: UserOrderV1Request.Create,
    ): ApiResponse<UserOrderV1Response.Created> {
        val userId = userAuthenticateUseCase.authenticateAndGetId(loginId, password)
        return createUseCase.create(request.toCommand(userId, idempotencyKey))
            .let { UserOrderV1Response.Created.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getList(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
        pageRequest: PageRequest,
    ): ApiResponse<PageResponse<UserOrderV1Response.ListItem>> {
        val userId = userAuthenticateUseCase.authenticateAndGetId(loginId, password)
        return listUseCase.getList(userId, from, to, pageRequest)
            .map { UserOrderV1Response.ListItem.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{orderId}")
    override fun getDetail(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable orderId: Long,
    ): ApiResponse<UserOrderV1Response.Detail> {
        val userId = userAuthenticateUseCase.authenticateAndGetId(loginId, password)
        return detailUseCase.getDetail(orderId, userId)
            .let { UserOrderV1Response.Detail.from(it) }
            .let { ApiResponse.success(it) }
    }
}
