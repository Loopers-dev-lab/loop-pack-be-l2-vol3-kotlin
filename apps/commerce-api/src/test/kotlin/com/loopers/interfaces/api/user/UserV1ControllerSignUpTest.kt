package com.loopers.interfaces.api.user

import com.loopers.application.user.UserFacade
import com.loopers.application.user.UserSignUpInfo
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("POST /api/v1/users - 회원가입")
@WebMvcTest(UserV1Controller::class)
class UserV1ControllerSignUpTest
@Autowired
constructor(
    private val mockMvc: MockMvc,
    @MockitoBean private val userFacade: UserFacade,
) {
    companion object {
        private const val ENDPOINT = "/api/v1/users"
    }

    @DisplayName("유효한 회원 정보로 가입 요청 시 201 Created와 loginId를 반환한다")
    @Test
    fun signUp_success_returns201WithLoginId() {
        // arrange
        given(userFacade.signUp(any())).willReturn(UserSignUpInfo(loginId = "testuser1"))

        val requestBody =
            """
            {
                "loginId": "testuser1",
                "password": "Password1!",
                "name": "홍길동",
                "birthDate": "1990-01-01",
                "email": "test@example.com"
            }
            """.trimIndent()

        // act & assert
        mockMvc.perform(
            post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.loginId").value("testuser1"))
    }

    @DisplayName("loginId가 빈 문자열이면 400 Bad Request를 반환한다")
    @Test
    fun signUp_invalidRequest_returns400() {
        // arrange
        val requestBody =
            """
            {
                "loginId": "",
                "password": "Password1!",
                "name": "홍길동",
                "birthDate": "1990-01-01",
                "email": "test@example.com"
            }
            """.trimIndent()

        // act & assert
        mockMvc.perform(
            post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        )
            .andExpect(status().isBadRequest)
    }

    @DisplayName("중복 loginId로 가입 요청 시 409 Conflict를 반환한다")
    @Test
    fun signUp_duplicateLoginId_returns409() {
        // arrange
        given(userFacade.signUp(any()))
            .willThrow(CoreException(ErrorType.USER_DUPLICATE_LOGIN_ID))

        val requestBody =
            """
            {
                "loginId": "testuser1",
                "password": "Password1!",
                "name": "홍길동",
                "birthDate": "1990-01-01",
                "email": "test@example.com"
            }
            """.trimIndent()

        // act & assert
        mockMvc.perform(
            post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        )
            .andExpect(status().isConflict)
    }
}
