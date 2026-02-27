package com.loopers.interfaces.api

import com.loopers.domain.admin.Admin
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import com.loopers.infrastructure.admin.AdminJpaRepository
import com.loopers.infrastructure.order.OrderJpaRepository
import com.loopers.interfaces.api.admin.order.AdminOrderV1Dto
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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
class AdminOrderV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val adminJpaRepository: AdminJpaRepository,
    private val orderJpaRepository: OrderJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    private lateinit var adminHeaders: HttpHeaders

    @BeforeEach
    fun setUp() {
        adminJpaRepository.save(Admin(ldap = "loopers.admin", name = "관리자"))
        adminHeaders = HttpHeaders()
        adminHeaders.set("X-Loopers-Ldap", "loopers.admin")
    }

    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("인증되지 않은 요청은 401 UNAUTHORIZED 응답을 받는다.")
    @Test
    fun returnsUnauthorized_whenNoLdapHeader() {
        // act
        val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
        val response = testRestTemplate.exchange("/api-admin/v1/orders", HttpMethod.GET, HttpEntity<Any>(Unit), responseType)

        // assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @DisplayName("GET /api-admin/v1/orders")
    @Nested
    inner class GetOrders {
        @DisplayName("주문 목록을 페이지네이션하여 조회한다.")
        @Test
        fun returnsOrderList() {
            // arrange
            orderJpaRepository.save(
                Order(
                    userId = 1L,
                    items = listOf(OrderItem(productId = 1L, productName = "에어맥스", productPrice = 139000, quantity = 1)),
                ),
            )
            orderJpaRepository.save(
                Order(
                    userId = 2L,
                    items = listOf(OrderItem(productId = 2L, productName = "에어포스", productPrice = 119000, quantity = 2)),
                ),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<AdminOrderV1Dto.OrderResponse>>>() {}
            val response = testRestTemplate.exchange("/api-admin/v1/orders?page=0&size=20", HttpMethod.GET, HttpEntity<Any>(Unit, adminHeaders), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.content).hasSize(2) },
                { assertThat(response.body?.data?.totalElements).isEqualTo(2) },
            )
        }
    }

    @DisplayName("GET /api-admin/v1/orders/{orderId}")
    @Nested
    inner class GetOrder {
        @DisplayName("존재하는 주문 ID를 주면, 주문 상세 정보를 반환한다.")
        @Test
        fun returnsOrderInfo_whenOrderExists() {
            // arrange
            val order = orderJpaRepository.save(
                Order(
                    userId = 1L,
                    items = listOf(OrderItem(productId = 1L, productName = "에어맥스", productPrice = 139000, quantity = 2)),
                ),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<AdminOrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange("/api-admin/v1/orders/${order.id}", HttpMethod.GET, HttpEntity<Any>(Unit, adminHeaders), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.id).isEqualTo(order.id) },
                { assertThat(response.body?.data?.userId).isEqualTo(1L) },
                { assertThat(response.body?.data?.totalPrice).isEqualTo(139000 * 2L) },
                { assertThat(response.body?.data?.items).hasSize(1) },
            )
        }

        @DisplayName("존재하지 않는 주문 ID를 주면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun throwsNotFound_whenOrderNotExists() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<AdminOrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange("/api-admin/v1/orders/999", HttpMethod.GET, HttpEntity<Any>(Unit, adminHeaders), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
}
