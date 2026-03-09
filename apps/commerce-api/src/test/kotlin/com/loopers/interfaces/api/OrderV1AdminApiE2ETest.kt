package com.loopers.interfaces.api

import com.loopers.domain.order.OrderItemModel
import com.loopers.domain.order.OrderModel
import com.loopers.domain.user.Email
import com.loopers.domain.user.Password
import com.loopers.domain.user.UserModel
import com.loopers.domain.user.Username
import com.loopers.infrastructure.order.OrderItemJpaRepository
import com.loopers.infrastructure.order.OrderJpaRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.interfaces.api.order.OrderV1AdminDto
import com.loopers.utils.DatabaseCleanUp
import java.math.BigDecimal
import java.time.ZonedDateTime
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderV1AdminApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val orderJpaRepository: OrderJpaRepository,
    private val orderItemJpaRepository: OrderItemJpaRepository,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val LDAP_HEADER = "loopers.admin"
        private const val ENDPOINT_BASE = "/api-admin/v1/orders"
        private const val DEFAULT_USERNAME = "testuser"
        private const val DEFAULT_PASSWORD = "password1234"
        private const val DEFAULT_NAME = "테스트유저"
        private const val DEFAULT_EMAIL = "test@example.com"
        private val DEFAULT_BIRTH_DATE: ZonedDateTime = ZonedDateTime.parse("1990-01-01T00:00:00+09:00")
        private val DEFAULT_TOTAL_PRICE = BigDecimal("258000")
        private const val DEFAULT_PRODUCT_NAME = "에어맥스 90"
        private const val DEFAULT_QUANTITY = 2
        private val DEFAULT_ITEM_PRICE = BigDecimal("129000")
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createUser(username: String = DEFAULT_USERNAME): UserModel {
        return userJpaRepository.save(
            UserModel(
                username = Username.of(username),
                password = Password.of(DEFAULT_PASSWORD, DEFAULT_BIRTH_DATE),
                name = DEFAULT_NAME,
                email = Email.of(DEFAULT_EMAIL),
                birthDate = DEFAULT_BIRTH_DATE,
            ),
        )
    }

    private fun createOrder(userId: Long, totalPrice: BigDecimal = DEFAULT_TOTAL_PRICE): OrderModel {
        return orderJpaRepository.save(
            OrderModel(userId = userId, totalPrice = totalPrice),
        )
    }

    private fun createOrderItem(
        orderId: Long,
        productId: Long = 1L,
        productName: String = DEFAULT_PRODUCT_NAME,
        quantity: Int = DEFAULT_QUANTITY,
        price: BigDecimal = DEFAULT_ITEM_PRICE,
    ): OrderItemModel {
        return orderItemJpaRepository.save(
            OrderItemModel(
                orderId = orderId,
                productId = productId,
                productName = productName,
                quantity = quantity,
                price = price,
            ),
        )
    }

    private fun createAuthAdminHeader(): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Loopers-Ldap", LDAP_HEADER)
        }
    }

    @DisplayName("GET /api-admin/v1/orders")
    @Nested
    inner class GetOrders {
        @DisplayName("주문 목록을 조회하면, 200 OK와 Slice 페이지네이션 응답을 반환한다.")
        @Test
        fun returnsOrderSliceAndOk() {
            // arrange
            val user = createUser()
            createOrder(userId = user.id)
            createOrder(userId = user.id, totalPrice = BigDecimal("100000"))
            val headers = createAuthAdminHeader()
            val url = "$ENDPOINT_BASE?page=0&size=10"

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1AdminDto.OrderSliceResponse>>() {}
            val response = testRestTemplate.exchange(url, HttpMethod.GET, HttpEntity(null, headers), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.content).hasSize(2) },
                { assertThat(response.body?.data?.hasNext).isFalse() },
            )
        }
    }

    @DisplayName("GET /api-admin/v1/orders/{orderId}")
    @Nested
    inner class GetOrder {
        @DisplayName("유효한 주문이면, 200 OK와 주문 상세 정보를 반환한다.")
        @Test
        fun returnsOrderDetailAndOkWhenOrderExists() {
            // arrange
            val user = createUser()
            val order = createOrder(userId = user.id)
            createOrderItem(orderId = order.id)
            val headers = createAuthAdminHeader()
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1AdminDto.OrderDetailResponse>>() {}

            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT_BASE/${order.id}",
                HttpMethod.GET,
                HttpEntity(null, headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.id).isEqualTo(order.id) },
                { assertThat(response.body?.data?.username).isEqualTo(DEFAULT_USERNAME) },
                { assertThat(response.body?.data?.totalPrice).isEqualByComparingTo(DEFAULT_TOTAL_PRICE) },
                { assertThat(response.body?.data?.orderItems).hasSize(1) },
                { assertThat(response.body?.data?.orderItems?.get(0)?.productName).isEqualTo(DEFAULT_PRODUCT_NAME) },
            )
        }

        @DisplayName("존재하지 않는 주문이면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFoundWhenOrderDoesNotExist() {
            // arrange
            val headers = createAuthAdminHeader()
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}

            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT_BASE/999",
                HttpMethod.GET,
                HttpEntity(null, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("인증 누락")
    @Nested
    inner class Unauthorized {
        @DisplayName("X-Loopers-Ldap 헤더 없이 요청하면, 401 UNAUTHORIZED를 반환한다.")
        @Test
        fun returnsUnauthorizedWhenLdapHeaderIsMissing() {
            // arrange
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}

            // act
            val response = testRestTemplate.exchange(
                ENDPOINT_BASE,
                HttpMethod.GET,
                HttpEntity(null, HttpHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }
}
