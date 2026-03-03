package com.loopers.interfaces.api

import com.loopers.application.catalog.AdminRegisterProductUseCase
import com.loopers.application.catalog.RegisterProductCriteria
import com.loopers.application.catalog.RegisterProductResult
import com.loopers.domain.catalog.BrandInfo
import com.loopers.domain.catalog.BrandService
import com.loopers.domain.catalog.RegisterBrandCommand
import com.loopers.domain.user.RegisterCommand
import com.loopers.domain.user.UserService
import com.loopers.interfaces.api.like.ProductLikeV1Dto
import com.loopers.utils.DatabaseCleanUp
import java.math.BigDecimal
import java.time.ZoneId
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
class ProductLikeV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandService: BrandService,
    private val adminRegisterProductUseCase: AdminRegisterProductUseCase,
    private val userService: UserService,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val LIKE_ENDPOINT = "/api/v1/products"
        private const val USER_LIKES_ENDPOINT = "/api/v1/users"

        private const val DEFAULT_BRAND_NAME = "나이키"
        private const val DEFAULT_PRODUCT_NAME = "에어맥스 90"
        private const val DEFAULT_QUANTITY = 100
        private val DEFAULT_PRICE = BigDecimal("129000")

        private const val DEFAULT_USERNAME = "testuser"
        private const val DEFAULT_PASSWORD = "password1234!"
        private const val DEFAULT_NAME = "테스트유저"
        private const val DEFAULT_EMAIL = "test@loopers.com"
        private val DEFAULT_BIRTH_DATE = ZonedDateTime.of(1995, 5, 29, 0, 0, 0, 0, ZoneId.of("Asia/Seoul"))
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun registerUser(): Long {
        val user = userService.register(
            RegisterCommand(
                username = DEFAULT_USERNAME,
                password = DEFAULT_PASSWORD,
                name = DEFAULT_NAME,
                email = DEFAULT_EMAIL,
                birthDate = DEFAULT_BIRTH_DATE,
            ),
        )
        return user.id
    }

    private fun registerBrand(name: String = DEFAULT_BRAND_NAME): BrandInfo {
        return brandService.register(RegisterBrandCommand(name = name))
    }

    private fun registerProduct(
        brandId: Long,
        name: String = DEFAULT_PRODUCT_NAME,
        price: BigDecimal = DEFAULT_PRICE,
    ): RegisterProductResult {
        return adminRegisterProductUseCase.execute(
            RegisterProductCriteria(
                brandId = brandId,
                name = name,
                quantity = DEFAULT_QUANTITY,
                price = price,
            ),
        )
    }

    private fun createAuthHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Loopers-LoginId", DEFAULT_USERNAME)
            set("X-Loopers-LoginPw", DEFAULT_PASSWORD)
        }
    }

    @DisplayName("POST /api/v1/products/{productId}/likes")
    @Nested
    inner class LikeProduct {

        @DisplayName("유효한 정보가 주어지면, 201 CREATED를 반환한다.")
        @Test
        fun returnsCreatedWhenValidInfoIsProvided() {
            // arrange
            registerUser()
            val brand = registerBrand()
            val product = registerProduct(brandId = brand.id)
            val headers = createAuthHeaders()

            // act
            val response = testRestTemplate.exchange(
                "$LIKE_ENDPOINT/${product.id}/likes",
                HttpMethod.POST,
                HttpEntity(null, headers),
                object : ParameterizedTypeReference<Any>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        }

        @DisplayName("이미 좋아요한 상품이면, 409 CONFLICT를 반환한다.")
        @Test
        fun returnsConflictWhenAlreadyLiked() {
            // arrange
            registerUser()
            val brand = registerBrand()
            val product = registerProduct(brandId = brand.id)
            val headers = createAuthHeaders()
            testRestTemplate.exchange(
                "$LIKE_ENDPOINT/${product.id}/likes",
                HttpMethod.POST,
                HttpEntity(null, headers),
                object : ParameterizedTypeReference<Any>() {},
            )

            // act
            val response = testRestTemplate.exchange(
                "$LIKE_ENDPOINT/${product.id}/likes",
                HttpMethod.POST,
                HttpEntity(null, headers),
                object : ParameterizedTypeReference<Any>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }
    }

    @DisplayName("DELETE /api/v1/products/{productId}/likes")
    @Nested
    inner class UnlikeProduct {

        @DisplayName("좋아요를 취소하면, 204 NO_CONTENT를 반환한다.")
        @Test
        fun returnsNoContentWhenUnlikeSucceeds() {
            // arrange
            registerUser()
            val brand = registerBrand()
            val product = registerProduct(brandId = brand.id)
            val headers = createAuthHeaders()
            testRestTemplate.exchange(
                "$LIKE_ENDPOINT/${product.id}/likes",
                HttpMethod.POST,
                HttpEntity(null, headers),
                object : ParameterizedTypeReference<Any>() {},
            )

            // act
            val response = testRestTemplate.exchange(
                "$LIKE_ENDPOINT/${product.id}/likes",
                HttpMethod.DELETE,
                HttpEntity(null, headers),
                object : ParameterizedTypeReference<Any>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        }

        @DisplayName("좋아요하지 않은 상품이면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFoundWhenLikeDoesNotExist() {
            // arrange
            registerUser()
            val headers = createAuthHeaders()

            // act
            val response = testRestTemplate.exchange(
                "$LIKE_ENDPOINT/999/likes",
                HttpMethod.DELETE,
                HttpEntity(null, headers),
                object : ParameterizedTypeReference<Any>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("GET /api/v1/users/{userId}/likes")
    @Nested
    inner class GetLikedProducts {

        @DisplayName("좋아요한 상품 목록을 조회하면, 200 OK와 Slice 응답을 반환한다.")
        @Test
        fun returnsOkWithLikedProductSlice() {
            // arrange
            val userId = registerUser()
            val brand = registerBrand()
            val product = registerProduct(brandId = brand.id)
            val headers = createAuthHeaders()
            testRestTemplate.exchange(
                "$LIKE_ENDPOINT/${product.id}/likes",
                HttpMethod.POST,
                HttpEntity(null, headers),
                object : ParameterizedTypeReference<Any>() {},
            )
            val responseType =
                object : ParameterizedTypeReference<ApiResponse<ProductLikeV1Dto.LikedProductSliceResponse>>() {}

            // act
            val response = testRestTemplate.exchange(
                "$USER_LIKES_ENDPOINT/$userId/likes?page=0&size=10",
                HttpMethod.GET,
                HttpEntity(null, headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.content).hasSize(1) },
                { assertThat(response.body?.data?.content?.get(0)?.name).isEqualTo(DEFAULT_PRODUCT_NAME) },
                { assertThat(response.body?.data?.content?.get(0)?.brandName).isEqualTo(DEFAULT_BRAND_NAME) },
                { assertThat(response.body?.data?.hasNext).isFalse() },
            )
        }

        @DisplayName("다른 사용자의 좋아요 목록을 조회하면, 401 UNAUTHORIZED를 반환한다.")
        @Test
        fun returnsUnauthorizedWhenUserIdMismatch() {
            // arrange
            registerUser()
            val headers = createAuthHeaders()
            val responseType =
                object : ParameterizedTypeReference<ApiResponse<ProductLikeV1Dto.LikedProductSliceResponse>>() {}

            // act
            val response = testRestTemplate.exchange(
                "$USER_LIKES_ENDPOINT/999/likes?page=0&size=10",
                HttpMethod.GET,
                HttpEntity(null, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("인증 헤더가 없으면, 401 UNAUTHORIZED를 반환한다.")
        @Test
        fun returnsUnauthorizedWhenNoAuthHeaders() {
            // arrange
            val responseType =
                object : ParameterizedTypeReference<ApiResponse<ProductLikeV1Dto.LikedProductSliceResponse>>() {}

            // act
            val response = testRestTemplate.exchange(
                "$USER_LIKES_ENDPOINT/1/likes?page=0&size=10",
                HttpMethod.GET,
                HttpEntity(null, HttpHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }
}
