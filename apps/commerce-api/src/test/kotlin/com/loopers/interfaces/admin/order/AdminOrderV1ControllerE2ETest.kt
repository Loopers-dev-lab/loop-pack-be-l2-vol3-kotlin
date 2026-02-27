package com.loopers.interfaces.admin.order

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Admin Order V1 API E2E Test")
class AdminOrderV1ControllerE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    companion object {
        private const val ADMIN_ENDPOINT = "/api-admin/v1/orders"

        private fun createAdminHeaders(): HttpHeaders {
            val headers = HttpHeaders()
            headers["X-LDAP-Username"] = "admin"
            headers["X-LDAP-Role"] = "ADMIN"
            return headers
        }
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("유효한 page와 size로 주문 목록을 조회할 수 있다")
    @Test
    fun retrievesOrders_withValidParameters() {
        // act
        val headers = createAdminHeaders()
        val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<AdminOrderV1Dto.OrderResponse>>>() {}
        val response = testRestTemplate.exchange(
            "$ADMIN_ENDPOINT?page=0&size=20",
            HttpMethod.GET,
            HttpEntity<Any>(null, headers),
            responseType,
        )

        // assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data).isNotNull
    }

    @DisplayName("음수 page 파라미터로는 요청할 수 없다")
    @Test
    fun failsToRetrieveOrders_whenPageIsNegative() {
        // act
        val headers = createAdminHeaders()
        val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<AdminOrderV1Dto.OrderResponse>>>() {}
        val response = testRestTemplate.exchange(
            "$ADMIN_ENDPOINT?page=-1&size=20",
            HttpMethod.GET,
            HttpEntity<Any>(null, headers),
            responseType,
        )

        // assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @DisplayName("유효하지 않은 size 파라미터로는 요청할 수 없다")
    @Test
    fun failsToRetrieveOrders_whenSizeIsInvalid() {
        // act
        val headers = createAdminHeaders()
        val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<AdminOrderV1Dto.OrderResponse>>>() {}
        val response = testRestTemplate.exchange(
            "$ADMIN_ENDPOINT?page=0&size=30",
            HttpMethod.GET,
            HttpEntity<Any>(null, headers),
            responseType,
        )

        // assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @DisplayName("size=50으로 주문 목록을 조회할 수 있다")
    @Test
    fun retrievesOrders_withSize50() {
        // act
        val headers = createAdminHeaders()
        val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<AdminOrderV1Dto.OrderResponse>>>() {}
        val response = testRestTemplate.exchange(
            "$ADMIN_ENDPOINT?page=0&size=50",
            HttpMethod.GET,
            HttpEntity<Any>(null, headers),
            responseType,
        )

        // assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @DisplayName("size=100으로 주문 목록을 조회할 수 있다")
    @Test
    fun retrievesOrders_withSize100() {
        // act
        val headers = createAdminHeaders()
        val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<AdminOrderV1Dto.OrderResponse>>>() {}
        val response = testRestTemplate.exchange(
            "$ADMIN_ENDPOINT?page=0&size=100",
            HttpMethod.GET,
            HttpEntity<Any>(null, headers),
            responseType,
        )

        // assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @DisplayName("존재하지 않는 주문은 404를 반환한다")
    @Test
    fun failsToRetrieveOrder_whenOrderNotFound() {
        // act
        val headers = createAdminHeaders()
        val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
        val response = testRestTemplate.exchange(
            "$ADMIN_ENDPOINT/99999",
            HttpMethod.GET,
            HttpEntity<Any>(null, headers),
            responseType,
        )

        // assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }
}
