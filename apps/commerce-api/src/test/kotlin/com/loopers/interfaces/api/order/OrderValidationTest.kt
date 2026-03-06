package com.loopers.interfaces.api.order

import com.loopers.interfaces.api.order.dto.OrderV1Dto
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
@DisplayName("OrderV1Controller - @Valid 인터페이스 어노테이션 상속 검증")
class OrderValidationTest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    companion object {
        private const val ENDPOINT_ORDERS = "/api/v1/orders"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun signUp(loginId: String = "testuser1") {
        val request = UserV1Dto.SignUpRequest(
            loginId = loginId,
            password = "Password1!",
            name = "홍길동",
            birthDate = LocalDate.of(1990, 1, 15),
            email = "$loginId@example.com",
        )
        val response = testRestTemplate.exchange(
            "/api/v1/users/sign-up",
            HttpMethod.POST,
            HttpEntity(request),
            object : ParameterizedTypeReference<ApiResponse<Any>>() {},
        )
        assertThat(response.statusCode.is2xxSuccessful)
            .describedAs("회원가입 API 호출이 성공해야 합니다: ${response.statusCode}")
            .isTrue()
    }

    private fun authHeaders(loginId: String = "testuser1"): HttpHeaders {
        return HttpHeaders().apply {
            set(HEADER_LOGIN_ID, loginId)
            set(HEADER_LOGIN_PW, "Password1!")
            set("Content-Type", "application/json")
        }
    }

    private fun createOrder(request: Any): org.springframework.http.ResponseEntity<ApiResponse<Any>> {
        return testRestTemplate.exchange(
            ENDPOINT_ORDERS,
            HttpMethod.POST,
            HttpEntity(request, authHeaders()),
            object : ParameterizedTypeReference<ApiResponse<Any>>() {},
        )
    }

    @Nested
    @DisplayName("POST /api/v1/orders - @Valid 인터페이스 상속 여부 확인")
    inner class CreateOrderValidation {

        @Test
        @DisplayName("items가 빈 리스트이면 400을 반환한다 (ApiSpec의 @Valid + @NotEmpty 동작 확인)")
        fun createOrder_emptyItems_returns400() {
            // arrange
            signUp()
            val request = OrderV1Dto.CreateOrderRequest(
                items = emptyList(),
            )

            // act
            val response = createOrder(request)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body?.meta?.message).contains("items")
        }

        @Test
        @DisplayName("items 내 quantity가 0이면 400을 반환한다 (ApiSpec의 @Valid + items 내부 @Min 동작 확인)")
        fun createOrder_itemWithZeroQuantity_returns400() {
            // arrange
            signUp()
            val request = OrderV1Dto.CreateOrderRequest(
                items = listOf(
                    OrderV1Dto.CreateOrderItemRequest(productId = 1L, quantity = 0),
                ),
            )

            // act
            val response = createOrder(request)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body?.meta?.message).contains("quantity")
        }

        @Test
        @DisplayName("issuedCouponId가 0이면 400을 반환한다 (ApiSpec의 @Min(1) 동작 확인)")
        fun createOrder_issuedCouponIdZero_returns400() {
            // arrange
            signUp()
            val request = OrderV1Dto.CreateOrderRequest(
                items = listOf(
                    OrderV1Dto.CreateOrderItemRequest(productId = 1L, quantity = 1),
                ),
                issuedCouponId = 0L,
            )

            // act
            val response = createOrder(request)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body?.meta?.message).contains("issuedCouponId")
        }
    }
}
