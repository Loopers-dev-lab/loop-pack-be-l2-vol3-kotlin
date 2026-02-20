package com.loopers.interfaces.api.order

import com.loopers.interfaces.api.order.dto.OrderV1Dto
import com.loopers.interfaces.api.point.dto.PointV1Dto
import com.loopers.interfaces.api.product.dto.ProductAdminV1Dto
import com.loopers.interfaces.api.user.dto.UserV1Dto
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.HEADER_LDAP
import com.loopers.interfaces.support.HEADER_LOGIN_ID
import com.loopers.interfaces.support.HEADER_LOGIN_PW
import com.loopers.interfaces.support.LDAP_ADMIN_VALUE
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
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderV1ApiE2ETest @Autowired constructor(
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

    private fun adminHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set(HEADER_LDAP, LDAP_ADMIN_VALUE)
            set("Content-Type", "application/json")
        }
    }

    private fun chargePoints(amount: Long) {
        testRestTemplate.exchange(
            "/api/v1/users/points/charge?amount=$amount",
            HttpMethod.POST,
            HttpEntity<Any>(authHeaders()),
            object : ParameterizedTypeReference<ApiResponse<Any>>() {},
        )
    }

    private fun createBrand(): Long {
        val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
        val response = testRestTemplate.exchange(
            "/api-admin/v1/brands?name={name}",
            HttpMethod.POST,
            HttpEntity<Void>(adminHeaders()),
            responseType,
            "나이키",
        )
        return (response.body!!.data!!["id"] as Number).toLong()
    }

    private fun createProduct(brandId: Long, name: String, price: BigDecimal, stock: Int): Long {
        val request = ProductAdminV1Dto.CreateProductRequest(
            brandId = brandId,
            name = name,
            price = price,
            stock = stock,
        )
        val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
        val response = testRestTemplate.exchange(
            "/api-admin/v1/products",
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            responseType,
        )
        return (response.body!!.data!!["id"] as Number).toLong()
    }

    @Nested
    @DisplayName("POST /api/v1/orders")
    inner class CreateOrder {

        @Test
        @DisplayName("회원가입 → 포인트 충전 → 상품 등록 → 주문 생성이 성공한다")
        fun createOrder_fullFlow_success() {
            // arrange
            signUp()
            chargePoints(500000)
            val brandId = createBrand()
            val productId = createProduct(brandId, "에어맥스 90", BigDecimal("129000"), 100)

            val orderRequest = OrderV1Dto.CreateOrderRequest(
                items = listOf(OrderV1Dto.CreateOrderItemRequest(productId = productId, quantity = 2)),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                HttpEntity(orderRequest, authHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.totalPrice).isEqualByComparingTo(BigDecimal("258000")) },
                { assertThat(response.body?.data?.items).hasSize(1) },
                { assertThat(response.body?.data?.status?.name).isEqualTo("CREATED") },
            )

            // 재고 차감 확인
            val productResponseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val productResponse = testRestTemplate.exchange(
                "/api-admin/v1/products/$productId",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                productResponseType,
            )
            assertThat((productResponse.body!!.data!!["stock"] as Number).toInt()).isEqualTo(98)

            // 포인트 차감 확인
            val balanceType = object : ParameterizedTypeReference<ApiResponse<PointV1Dto.BalanceResponse>>() {}
            val balanceResponse = testRestTemplate.exchange(
                "/api/v1/users/points",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                balanceType,
            )
            assertThat(balanceResponse.body?.data?.balance).isEqualTo(242000)
        }

        @Test
        @DisplayName("포인트가 부족하면 주문이 실패한다")
        fun createOrder_insufficientPoints_fails() {
            // arrange
            signUp()
            chargePoints(1000)
            val brandId = createBrand()
            val productId = createProduct(brandId, "에어맥스 90", BigDecimal("129000"), 100)

            val orderRequest = OrderV1Dto.CreateOrderRequest(
                items = listOf(OrderV1Dto.CreateOrderItemRequest(productId = productId, quantity = 1)),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                HttpEntity(orderRequest, authHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }
}
