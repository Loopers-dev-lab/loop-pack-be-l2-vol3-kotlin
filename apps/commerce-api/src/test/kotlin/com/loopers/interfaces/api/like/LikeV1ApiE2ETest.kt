package com.loopers.interfaces.api.like

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.brand.BrandV1Dto
import com.loopers.interfaces.api.product.ProductAdminV1Dto
import com.loopers.interfaces.api.product.ProductV1Dto
import com.loopers.interfaces.api.user.UserV1Dto
import com.loopers.infrastructure.user.UserJpaRepository
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
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LikeV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    companion object {
        private const val TEST_LOGIN_ID = "testuser1"
        private const val TEST_PASSWORD = "Password1!"
        private const val ADMIN_LDAP = "loopers.admin"
    }

    private var testUserId: Long = 0
    private var testProductId: Long = 0

    @BeforeEach
    fun setUp() {
        // 유저 생성
        createTestUser()
        testUserId = userJpaRepository.findByLoginId(TEST_LOGIN_ID)!!.id

        // 브랜드 + 상품 생성
        val brandId = createTestBrand()!!
        testProductId = createTestProduct(brandId)!!
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun authHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Loopers-LoginId", TEST_LOGIN_ID)
            set("X-Loopers-LoginPw", TEST_PASSWORD)
        }
    }

    private fun adminHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Loopers-Ldap", ADMIN_LDAP)
        }
    }

    private fun createTestUser() {
        val request = UserV1Dto.SignUpRequest(
            loginId = TEST_LOGIN_ID,
            password = TEST_PASSWORD,
            name = "테스트유저",
            birthDate = LocalDate.of(1990, 1, 15),
            email = "test@example.com",
        )
        testRestTemplate.exchange(
            "/api/v1/users",
            HttpMethod.POST,
            HttpEntity(request),
            object : ParameterizedTypeReference<ApiResponse<Any>>() {},
        )
    }

    private fun createTestBrand(): Long? {
        val request = BrandV1Dto.CreateRequest(name = "나이키", description = "스포츠 브랜드")
        val response = testRestTemplate.exchange(
            "/api-admin/v1/brands",
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            object : ParameterizedTypeReference<ApiResponse<BrandV1Dto.BrandAdminResponse>>() {},
        )
        return response.body?.data?.id
    }

    private fun createTestProduct(brandId: Long): Long? {
        val request = ProductAdminV1Dto.CreateRequest(
            brandId = brandId,
            name = "에어맥스 90",
            price = BigDecimal("129000"),
            stock = 100,
            description = "나이키 에어맥스 90",
            imageUrl = null,
        )
        val response = testRestTemplate.exchange(
            "/api-admin/v1/products",
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            object : ParameterizedTypeReference<ApiResponse<ProductAdminV1Dto.ProductAdminResponse>>() {},
        )
        return response.body?.data?.id
    }

    @DisplayName("POST /api/v1/products/{productId}/likes")
    @Nested
    inner class AddLike {

        @DisplayName("정상적인 요청이면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenAddLikeSucceeds() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/products/$testProductId/likes",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }

        @DisplayName("이미 좋아요한 상품에 다시 요청해도, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenAlreadyLiked() {
            // arrange
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            testRestTemplate.exchange(
                "/api/v1/products/$testProductId/likes",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/products/$testProductId/likes",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }

        @DisplayName("좋아요 등록 후 상품을 조회하면, likeCount가 1이다.")
        @Test
        fun increasesLikeCount_whenAddLikeSucceeds() {
            // arrange
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            testRestTemplate.exchange(
                "/api/v1/products/$testProductId/likes",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // act
            val productType = object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/products/$testProductId",
                HttpMethod.GET,
                null,
                productType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.likeCount).isEqualTo(1) },
            )
        }

        @DisplayName("존재하지 않는 상품이면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenProductNotExists() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/products/999/likes",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @DisplayName("인증 정보가 잘못되면, 401 UNAUTHORIZED를 반환한다.")
        @Test
        fun returnsUnauthorized_whenAuthFails() {
            // arrange
            val badHeaders = HttpHeaders().apply {
                set("X-Loopers-LoginId", TEST_LOGIN_ID)
                set("X-Loopers-LoginPw", "WrongPassword1!")
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/products/$testProductId/likes",
                HttpMethod.POST,
                HttpEntity<Any>(badHeaders),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    @DisplayName("DELETE /api/v1/products/{productId}/likes")
    @Nested
    inner class CancelLike {

        @DisplayName("좋아요를 취소하면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenCancelSucceeds() {
            // arrange - 먼저 좋아요 등록
            val addType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            testRestTemplate.exchange(
                "/api/v1/products/$testProductId/likes",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                addType,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/products/$testProductId/likes",
                HttpMethod.DELETE,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }

        @DisplayName("좋아요 취소 후 상품을 조회하면, likeCount가 0이다.")
        @Test
        fun decreasesLikeCount_whenCancelLikeSucceeds() {
            // arrange
            val type = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            testRestTemplate.exchange("/api/v1/products/$testProductId/likes", HttpMethod.POST, HttpEntity<Any>(authHeaders()), type)
            testRestTemplate.exchange("/api/v1/products/$testProductId/likes", HttpMethod.DELETE, HttpEntity<Any>(authHeaders()), type)

            // act
            val productType = object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/products/$testProductId",
                HttpMethod.GET,
                null,
                productType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.likeCount).isEqualTo(0) },
            )
        }

        @DisplayName("좋아요가 없어도, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenNoLikeExists() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/products/$testProductId/likes",
                HttpMethod.DELETE,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }

        @DisplayName("취소 후 다시 좋아요하면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenReLikeAfterCancel() {
            // arrange
            val type = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            testRestTemplate.exchange("/api/v1/products/$testProductId/likes", HttpMethod.POST, HttpEntity<Any>(authHeaders()), type)
            testRestTemplate.exchange("/api/v1/products/$testProductId/likes", HttpMethod.DELETE, HttpEntity<Any>(authHeaders()), type)

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/products/$testProductId/likes",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                type,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }
    }

    @DisplayName("GET /api/v1/users/{userId}/likes")
    @Nested
    inner class GetUserLikes {

        @DisplayName("본인의 좋아요 목록을 조회하면, 200 OK와 목록을 반환한다.")
        @Test
        fun returnsOk_whenQueryOwnLikes() {
            // arrange - 좋아요 등록
            val type = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            testRestTemplate.exchange("/api/v1/products/$testProductId/likes", HttpMethod.POST, HttpEntity<Any>(authHeaders()), type)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<List<Map<String, Any>>>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/users/$testUserId/likes",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isNotNull() },
                { assertThat(response.body?.data).hasSize(1) },
            )
        }

        @DisplayName("다른 유저의 좋아요 목록을 조회하면, 403 FORBIDDEN을 반환한다.")
        @Test
        fun returnsForbidden_whenQueryOtherUserLikes() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/users/999/likes",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @DisplayName("좋아요가 없으면, 빈 목록을 반환한다.")
        @Test
        fun returnsEmptyList_whenNoLikes() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<List<Map<String, Any>>>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/users/$testUserId/likes",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isEmpty() },
            )
        }
    }
}
