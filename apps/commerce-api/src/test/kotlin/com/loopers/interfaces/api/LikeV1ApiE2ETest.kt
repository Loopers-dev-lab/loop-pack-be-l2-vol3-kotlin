package com.loopers.interfaces.api

import com.loopers.interfaces.api.admin.brand.AdminBrandV1Dto
import com.loopers.interfaces.api.admin.product.AdminProductV1Dto
import com.loopers.interfaces.api.member.MemberV1Dto
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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
class LikeV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ADMIN_BRAND_ENDPOINT = "/api-admin/v1/brands"
        private const val ADMIN_PRODUCT_ENDPOINT = "/api-admin/v1/products"
        private const val MEMBER_ENDPOINT = "/api/v1/members"
        private const val HEADER_LDAP = "X-Loopers-Ldap"
        private const val LDAP_VALUE = "loopers.admin"
        private const val HEADER_LOGIN_ID = "X-Loopers-LoginId"
        private const val HEADER_LOGIN_PW = "X-Loopers-LoginPw"
    }

    private var productId: Long = 0
    private var memberId: Long = 0
    private val loginId = "testuser"
    private val password = "Password1!"

    @BeforeEach
    fun setUp() {
        // Register member
        val memberRequest = MemberV1Dto.RegisterRequest(
            loginId = loginId,
            password = password,
            name = "테스트",
            birthday = LocalDate.of(2000, 1, 1),
            email = "test@example.com",
        )
        testRestTemplate.exchange(
            MEMBER_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(memberRequest),
            object : ParameterizedTypeReference<ApiResponse<Void>>() {},
        )

        // Get member ID by accessing /me
        val meResponse = testRestTemplate.exchange(
            "$MEMBER_ENDPOINT/me",
            HttpMethod.GET,
            HttpEntity<Any>(memberHeaders()),
            object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {},
        )
        // We need the member ID. Since MemberResponse doesn't expose it, use header-based auth instead.
        // memberId is resolved from AuthenticatedMember in controller.
        // For Like E2E test, we use productId from admin API.

        // Create brand + product via admin
        val brandRequest = AdminBrandV1Dto.CreateRequest(
            name = "루퍼스",
            description = "테스트",
            imageUrl = "https://example.com/brand.jpg",
        )
        val brandResponse = testRestTemplate.exchange(
            ADMIN_BRAND_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(brandRequest, adminHeaders()),
            object : ParameterizedTypeReference<ApiResponse<AdminBrandV1Dto.BrandResponse>>() {},
        )
        val brandId = brandResponse.body!!.data!!.id

        val productRequest = AdminProductV1Dto.CreateRequest(
            brandId = brandId,
            name = "감성 티셔츠",
            description = "좋은 상품",
            price = 39000,
            stockQuantity = 100,
            imageUrl = "https://example.com/product.jpg",
        )
        val productResponse = testRestTemplate.exchange(
            ADMIN_PRODUCT_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(productRequest, adminHeaders()),
            object : ParameterizedTypeReference<ApiResponse<AdminProductV1Dto.ProductResponse>>() {},
        )
        productId = productResponse.body!!.data!!.id
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun adminHeaders(): HttpHeaders {
        return HttpHeaders().apply { set(HEADER_LDAP, LDAP_VALUE) }
    }

    private fun memberHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set(HEADER_LOGIN_ID, loginId)
            set(HEADER_LOGIN_PW, password)
        }
    }

    @DisplayName("POST /api/v1/products/{productId}/likes (좋아요 등록)")
    @Nested
    inner class Like {
        @DisplayName("인증된 사용자가 좋아요하면, 200 OK 응답을 받는다.")
        @Test
        fun returns200_whenAuthenticated() {
            // act
            val response = testRestTemplate.exchange(
                "/api/v1/products/$productId/likes",
                HttpMethod.POST,
                HttpEntity<Any>(memberHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }

        @DisplayName("이미 좋아요한 상품에 다시 좋아요하면, 멱등하게 200 OK 응답을 받는다.")
        @Test
        fun returns200Idempotently_whenAlreadyLiked() {
            // arrange
            testRestTemplate.exchange(
                "/api/v1/products/$productId/likes",
                HttpMethod.POST,
                HttpEntity<Any>(memberHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/products/$productId/likes",
                HttpMethod.POST,
                HttpEntity<Any>(memberHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }

        @DisplayName("인증 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returns401_whenNotAuthenticated() {
            // act
            val response = testRestTemplate.exchange(
                "/api/v1/products/$productId/likes",
                HttpMethod.POST,
                HttpEntity<Any>(HttpHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    @DisplayName("DELETE /api/v1/products/{productId}/likes (좋아요 취소)")
    @Nested
    inner class Unlike {
        @DisplayName("좋아요를 취소하면, 200 OK 응답을 받는다.")
        @Test
        fun returns200_whenUnlike() {
            // arrange
            testRestTemplate.exchange(
                "/api/v1/products/$productId/likes",
                HttpMethod.POST,
                HttpEntity<Any>(memberHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/products/$productId/likes",
                HttpMethod.DELETE,
                HttpEntity<Any>(memberHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }

        @DisplayName("좋아요하지 않은 상품의 좋아요를 취소하면, 멱등하게 200 OK 응답을 받는다.")
        @Test
        fun returns200Idempotently_whenNotLiked() {
            // act
            val response = testRestTemplate.exchange(
                "/api/v1/products/$productId/likes",
                HttpMethod.DELETE,
                HttpEntity<Any>(memberHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }
    }
}
