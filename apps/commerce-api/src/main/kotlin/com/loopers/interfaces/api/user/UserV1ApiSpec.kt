package com.loopers.interfaces.api.user

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AuthenticatedUser
import com.loopers.interfaces.api.auth.RequireAuth
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "User V1 API", description = "사용자 관리 API")
interface UserV1ApiSpec {

    @Operation(
        summary = "회원가입",
        description = "새로운 사용자를 등록합니다.",
    )
    fun register(
        @RequestBody request: UserV1Dto.UserRegisterRequest,
    ): ApiResponse<UserV1Dto.UserRegisterResponse>

    @Operation(
        summary = "사용자 정보 조회",
        description = "인증된 사용자의 정보를 조회합니다.",
        parameters = [
            Parameter(
                name = "X-Loopers-LoginId",
                description = "사용자 로그인 ID",
                required = true,
                schema = Schema(type = "string"),
                `in` = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER,
            ),
            Parameter(
                name = "X-Loopers-LoginPw",
                description = "사용자 비밀번호",
                required = true,
                schema = Schema(type = "string"),
                `in` = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER,
            ),
        ],
    )
    fun getUserInfo(
        @Parameter(hidden = true) @RequireAuth authenticatedUser: AuthenticatedUser,
    ): ApiResponse<UserV1Dto.UserInfoResponse>

    @Operation(
        summary = "비밀번호 변경",
        description = "인증된 사용자의 비밀번호를 변경합니다. 생년월일은 인증 컨텍스트에서 자동으로 가져와 검증에 사용됩니다.",
        parameters = [
            Parameter(
                name = "X-Loopers-LoginId",
                description = "사용자 로그인 ID",
                required = true,
                schema = Schema(type = "string"),
                `in` = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER,
            ),
            Parameter(
                name = "X-Loopers-LoginPw",
                description = "현재 비밀번호",
                required = true,
                schema = Schema(type = "string"),
                `in` = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER,
            ),
        ],
    )
    fun changePassword(
        @Parameter(hidden = true) @RequireAuth authenticatedUser: AuthenticatedUser,
        @RequestBody request: UserV1Dto.ChangePasswordRequest,
    ): ApiResponse<Unit>
}
