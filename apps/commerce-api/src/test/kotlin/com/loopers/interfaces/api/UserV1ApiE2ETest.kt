package com.loopers.interfaces.api

import com.loopers.interfaces.api.user.UserV1Dto
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT_REGISTER = "/api/v1/users/register"
    }

    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("POST /api/v1/users/register")
    @Nested
    inner class RegisterUser {
        @DisplayName("회원 가입이 성공할 경우, 생성된 유저 정보를 응답으로 반환한다.")
        @Test
        fun returnsUserInfo_whenRegisterUser() {
            //arrange
            val req = UserV1Dto.RegisterUserRequest(
                loginId = "testId",
                password = "testPassword",
                name = "testName",
                birth = "2026-01-31",
                email = "test@test.com",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.RegisterUserResponse>>() {}
            val res = testRestTemplate.exchange(ENDPOINT_REGISTER, HttpMethod.POST, HttpEntity(req), responseType)

            // assert
            assertAll(
                { assertThat(res.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(res.body?.data?.loginId).isEqualTo(req.loginId) },
                { assertThat(res.body?.data?.name).isEqualTo(req.name.substring(0, req.name.length - 1) + "*") },
                { assertThat(res.body?.data?.birth).isEqualTo(req.birth) },
                { assertThat(res.body?.data?.email).isEqualTo(req.email) },
            )
        }
    }
}
