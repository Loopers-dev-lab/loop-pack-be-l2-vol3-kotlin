package com.loopers.interfaces.api.like

import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.HEADER_LDAP
import com.loopers.interfaces.support.HEADER_LOGIN_ID
import com.loopers.interfaces.support.HEADER_LOGIN_PW
import com.loopers.interfaces.support.LDAP_ADMIN_VALUE
import com.loopers.interfaces.api.product.ProductAdminV1Dto
import com.loopers.interfaces.api.user.UserV1Dto
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
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LikeV1ApiE2ETest @Autowired constructor(
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
        }
    }

    private fun adminHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set(HEADER_LDAP, LDAP_ADMIN_VALUE)
            set("Content-Type", "application/json")
        }
    }

    private fun createBrandAndProduct(): Long {
        val brandRequest = mapOf("name" to "나이키")
        val brandResponseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
        val brandResponse = testRestTemplate.exchange(
            "/api-admin/v1/brands",
            HttpMethod.POST,
            HttpEntity(brandRequest, adminHeaders()),
            brandResponseType,
        )
        val brandId = (brandResponse.body!!.data!!["id"] as Number).toLong()

        val productRequest = ProductAdminV1Dto.CreateProductRequest(
            brandId = brandId,
            name = "에어맥스 90",
            price = BigDecimal("129000"),
            stock = 100,
        )
        val productResponseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
        val productResponse = testRestTemplate.exchange(
            "/api-admin/v1/products",
            HttpMethod.POST,
            HttpEntity(productRequest, adminHeaders()),
            productResponseType,
        )
        return (productResponse.body!!.data!!["id"] as Number).toLong()
    }

    @Nested
    @DisplayName("좋아요 멱등성 테스트")
    inner class LikeIdempotency {

        @Test
        @DisplayName("좋아요 2회 등록해도 likeCount는 1만 증가한다")
        fun addLikeTwice_likeCountIncreasesOnce() {
            // arrange
            signUp()
            val productId = createBrandAndProduct()

            // act - 좋아요 2회
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response1 = testRestTemplate.exchange(
                "/api/v1/likes/$productId",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )
            val response2 = testRestTemplate.exchange(
                "/api/v1/likes/$productId",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert
            assertThat(response1.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response2.statusCode).isEqualTo(HttpStatus.OK)

            // likeCount 확인 (상품 상세 조회)
            val productType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val productResponse = testRestTemplate.exchange(
                "/api/admin/v1/products/$productId",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                productType,
            )
            // 어드민 API로 likeCount 확인
            val adminProductType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val adminResponse = testRestTemplate.exchange(
                "/api-admin/v1/products/$productId",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                adminProductType,
            )
            assertThat((adminResponse.body!!.data!!["likeCount"] as Number).toInt()).isEqualTo(1)
        }

        @Test
        @DisplayName("좋아요 취소 2회해도 likeCount는 1만 감소한다")
        fun removeLikeTwice_likeCountDecreasesOnce() {
            // arrange
            signUp()
            val productId = createBrandAndProduct()

            // 좋아요 등록
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            testRestTemplate.exchange(
                "/api/v1/likes/$productId",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // act - 좋아요 취소 2회
            val delete1 = testRestTemplate.exchange(
                "/api/v1/likes/$productId",
                HttpMethod.DELETE,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )
            val delete2 = testRestTemplate.exchange(
                "/api/v1/likes/$productId",
                HttpMethod.DELETE,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert
            assertThat(delete1.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(delete2.statusCode).isEqualTo(HttpStatus.OK)

            val adminProductType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val adminResponse = testRestTemplate.exchange(
                "/api-admin/v1/products/$productId",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                adminProductType,
            )
            assertThat((adminResponse.body!!.data!!["likeCount"] as Number).toInt()).isEqualTo(0)
        }
    }
}
