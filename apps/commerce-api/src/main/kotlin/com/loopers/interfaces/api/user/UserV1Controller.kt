package com.loopers.interfaces.api.user

import com.loopers.application.api.productlike.ProductLikeFacade
import com.loopers.application.api.user.UserFacade
import com.loopers.domain.productlike.dto.LikedProductInfo
import com.loopers.domain.user.dto.SignUpCommand
import com.loopers.domain.user.dto.UserInfo
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserV1Controller(
    private val userFacade: UserFacade,
    private val productLikeFacade: ProductLikeFacade,
) : UserV1ApiSpec {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun signUp(@RequestBody @Valid signUpRequest: UserV1Dto.SignUpRequest): ApiResponse<Any> {
        val signUpCommand = SignUpCommand(
            loginId = signUpRequest.loginId,
            password = signUpRequest.password,
            name = signUpRequest.name,
            birthDate = signUpRequest.birthDate,
            email = signUpRequest.email,
        )
        userFacade.signUp(signUpCommand)
        return ApiResponse.success()
    }

    @GetMapping
    override fun findUserInfo(
        @RequestAttribute("userId") id: Long,
    ): ApiResponse<UserInfo> = userFacade.findUserInfo(id).let { ApiResponse.success(it) }

    @PutMapping("/password")
    override fun changePassword(
        @RequestAttribute("userId") id: Long,
        @RequestBody @Valid passwordChangeRequest: UserV1Dto.PasswordChangeRequest,
    ): ApiResponse<Any> {
        userFacade.changePassword(id, passwordChangeRequest.currentPassword, passwordChangeRequest.newPassword)
        return ApiResponse.success()
    }

    @GetMapping("/{userId}/likes")
    override fun getMyLikedProducts(
        @PathVariable userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<LikedProductInfo>> {
        if (size !in listOf(20, 50, 100)) {
            throw CoreException(ErrorType.BAD_REQUEST, "size는 20, 50, 100만 가능합니다")
        }

        if (page < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "page는 음수일 수 없습니다")
        }

        val pageable = PageRequest.of(page, size)
        val pageData = productLikeFacade.getMyLikedProducts(userId, pageable)
        return ApiResponse.success(data = PageResponse.from(pageData))
    }
}
