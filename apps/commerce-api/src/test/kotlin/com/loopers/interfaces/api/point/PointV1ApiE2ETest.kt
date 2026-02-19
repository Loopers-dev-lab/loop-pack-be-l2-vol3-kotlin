package com.loopers.interfaces.api.point

import com.loopers.interfaces.api.point.dto.PointV1Dto
import com.loopers.interfaces.api.user.dto.UserV1Dto
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.HEADER_LOGIN_ID
import com.loopers.interfaces.support.HEADER_LOGIN_PW
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
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PointV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun signUp() {
        val request = UserV1Dto.SignUpRequest(
            loginId = "testuser1",
            password = "Password1!",
            name = "홍길동",
            birthDate = LocalDate.of(1990, 1, 15),
            email = "test@example.com",
        )
        testRestTemplate.exchange(
            "/api/v1/users/sign-up",
            HttpMethod.POST,
            HttpEntity(request),
            object : ParameterizedTypeReference<ApiResponse<Any>>() {},
        )
    }

    private fun authHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set(HEADER_LOGIN_ID, "testuser1")
            set(HEADER_LOGIN_PW, "Password1!")
            set("Content-Type", "application/json")
        }
    }

    @Nested
    @DisplayName("포인트 충전 → 잔액 조회")
    inner class ChargeAndBalance {

        @Test
        @DisplayName("회원가입 → 충전 → 잔액 확인이 성공한다")
        fun chargeAndBalance_success() {
            // arrange
            signUp()

            val chargeRequest = PointV1Dto.ChargeRequest(amount = 50000)

            // act - 충전
            val chargeResponseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val chargeResponse = testRestTemplate.exchange(
                "/api/v1/users/points/charge",
                HttpMethod.POST,
                HttpEntity(chargeRequest, authHeaders()),
                chargeResponseType,
            )

            // assert - 충전 성공
            assertThat(chargeResponse.statusCode).isEqualTo(HttpStatus.OK)

            // act - 잔액 조회
            val balanceType = object : ParameterizedTypeReference<ApiResponse<PointV1Dto.BalanceResponse>>() {}
            val balanceResponse = testRestTemplate.exchange(
                "/api/v1/users/points",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                balanceType,
            )

            // assert - 잔액 확인
            assertAll(
                { assertThat(balanceResponse.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(balanceResponse.body?.data?.balance).isEqualTo(50000) },
            )
        }

        @Test
        @DisplayName("여러 번 충전하면 잔액이 누적된다")
        fun multipleCharges_accumulatesBalance() {
            // arrange
            signUp()

            // act
            val chargeResponseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            testRestTemplate.exchange(
                "/api/v1/users/points/charge",
                HttpMethod.POST,
                HttpEntity(PointV1Dto.ChargeRequest(amount = 10000), authHeaders()),
                chargeResponseType,
            )
            testRestTemplate.exchange(
                "/api/v1/users/points/charge",
                HttpMethod.POST,
                HttpEntity(PointV1Dto.ChargeRequest(amount = 20000), authHeaders()),
                chargeResponseType,
            )

            // assert
            val balanceType = object : ParameterizedTypeReference<ApiResponse<PointV1Dto.BalanceResponse>>() {}
            val balanceResponse = testRestTemplate.exchange(
                "/api/v1/users/points",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                balanceType,
            )
            assertThat(balanceResponse.body?.data?.balance).isEqualTo(30000)
        }

        @Test
        @DisplayName("1회 충전 한도(10,000,000)와 동일한 금액이면 충전에 성공한다")
        fun charge_exactMaxAmount_success() {
            // arrange
            signUp()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PointV1Dto.BalanceResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/users/points/charge",
                HttpMethod.POST,
                HttpEntity(PointV1Dto.ChargeRequest(amount = 10_000_000), authHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.balance).isEqualTo(10_000_000) },
            )
        }

        @Test
        @DisplayName("1회 충전 한도(10,000,000)를 초과하면 400 Bad Request 응답을 반환한다")
        fun charge_exceedMaxAmount_returnsBadRequest() {
            // arrange
            signUp()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/users/points/charge",
                HttpMethod.POST,
                HttpEntity(PointV1Dto.ChargeRequest(amount = 10_000_001), authHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }
}
