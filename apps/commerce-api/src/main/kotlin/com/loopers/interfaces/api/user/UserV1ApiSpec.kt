package com.loopers.interfaces.api.user

import com.loopers.domain.user.dto.UserInfo
import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "User V1 API", description = "회원 관련 API")
interface UserV1ApiSpec {

    @Operation(
        summary = "회원 가입",
        description = "로그인ID, 이름, 이메일, 생일로 회원 가입을 진행합니다.",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원 가입 성공"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)"),
        ],
    )
    fun signUp(
        @RequestBody(description = "회원 가입 요청 정보", required = true)
        signUpRequest: UserV1Dto.SignUpRequest,
    ): ApiResponse<Any>

    @Operation(
        summary = "회원 정보 조회",
        description = "로그인 ID, 이름, 생년월일, 이메일을 조회한다",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        ],
    )
    fun findUserInfo(
        @Parameter(description = "로그인한 사용자 ID", required = true)
        id: Long,
    ): ApiResponse<UserInfo>

    @Operation(
        summary = "비밀번호 변경",
        description = "기존 비밀번호 확인 후 새 비밀번호로 변경합니다. 비밀번호 RULE: 영문 대/소문자, 숫자, 특수문자 사용 가능. 생년월일 사용 불가. 현재 비밀번호와 동일한 비밀번호 사용 불가.",
        responses = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (기존 비밀번호 불일치, 비밀번호 규칙 위반 등)",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        ],
    )
    fun changePassword(
        @Parameter(description = "로그인한 사용자 ID", required = true)
        id: Long,
        @RequestBody(description = "비밀번호 변경 요청 정보", required = true)
        passwordChangeRequest: UserV1Dto.PasswordChangeRequest,
    ): ApiResponse<Any>
}
