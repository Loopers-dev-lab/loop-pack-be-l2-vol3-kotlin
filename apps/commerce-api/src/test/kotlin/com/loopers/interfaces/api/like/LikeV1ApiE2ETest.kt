package com.loopers.interfaces.api.like

import com.loopers.application.product.ProductFacade
import com.loopers.domain.brand.BrandService
import com.loopers.domain.user.BirthDate
import com.loopers.domain.user.Email
import com.loopers.domain.user.LoginId
import com.loopers.domain.user.Name
import com.loopers.domain.user.PasswordEncryptor
import com.loopers.domain.user.UserModel
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.interfaces.api.ApiResponse
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LikeV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandService: BrandService,
    private val productFacade: ProductFacade,
    private val userJpaRepository: UserJpaRepository,
    private val passwordEncryptor: PasswordEncryptor,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val PRODUCTS = "/api/v1/products"
        private const val USERS = "/api/v1/users"
        private const val TEST_PASSWORD = "Password123!"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createUser(loginId: String = "testuser") = userJpaRepository.save(
        UserModel(
            loginId = LoginId(loginId),
            encryptedPassword = passwordEncryptor.encrypt(TEST_PASSWORD),
            name = Name("홍길동"),
            birthDate = BirthDate("1990-01-01"),
            email = Email("$loginId@example.com"),
        )
    )

    private fun createBrand() = brandService.createBrand(
        name = "Nike",
        logoImageUrl = "logo.png",
        description = "테스트 브랜드",
        zipCode = "12345",
        roadAddress = "서울특별시 중구 테스트길 1",
        detailAddress = "1층",
        email = "nike@google.com",
        phoneNumber = "02-3783-4401",
        businessNumber = "123-45-67890",
    )

    private fun createProduct(brandId: Long) = productFacade.createProduct(
        brandId = brandId,
        name = "Air Max",
        imageUrl = "image.png",
        description = "설명",
        price = 50_000L,
        quantity = 100L,
    )

    private fun authHeaders(loginId: String = "testuser") = HttpHeaders().apply {
        set("X-Loopers-LoginId", loginId)
        set("X-Loopers-LoginPw", TEST_PASSWORD)
    }

    @DisplayName("POST /api/v1/products/{id}/likes")
    @Nested
    inner class Like {

        @Test
        @DisplayName("인증 헤더로 요청하면 200과 LikeResponse를 반환한다")
        fun like_whenValidAuth_thenReturnsLikeResponse() {
            val user = createUser()
            val brand = createBrand()
            val product = createProduct(brand.id)

            val responseType = object : ParameterizedTypeReference<ApiResponse<LikeV1Dto.LikeResponse>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCTS/${product.id}/likes",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.userId).isEqualTo(user.id) },
                { assertThat(response.body?.data?.productId).isEqualTo(product.id) },
            )
        }

        @Test
        @DisplayName("인증 헤더 없이 요청하면 401을 반환한다")
        fun like_whenNoAuth_thenReturns401() {
            val brand = createBrand()
            val product = createProduct(brand.id)

            val responseType = object : ParameterizedTypeReference<ApiResponse<LikeV1Dto.LikeResponse>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCTS/${product.id}/likes",
                HttpMethod.POST,
                HttpEntity<Any>(Unit),
                responseType,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    @DisplayName("DELETE /api/v1/products/{id}/likes")
    @Nested
    inner class Unlike {

        @Test
        @DisplayName("인증 헤더로 요청하면 200을 반환한다")
        fun unlike_whenValidAuth_thenReturns200() {
            createUser()
            val brand = createBrand()
            val product = createProduct(brand.id)

            // 먼저 좋아요 등록
            testRestTemplate.exchange(
                "$PRODUCTS/${product.id}/likes",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<LikeV1Dto.LikeResponse>>() {},
            )

            // 좋아요 취소
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCTS/${product.id}/likes",
                HttpMethod.DELETE,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }
    }

    @DisplayName("GET /api/v1/users/{userId}/likes")
    @Nested
    inner class GetLikedProducts {

        @Test
        @DisplayName("본인 좋아요 목록 조회 시 200과 목록을 반환한다")
        fun getLikedProducts_whenOwner_thenReturnsLikeList() {
            val user = createUser()
            val brand = createBrand()
            val product = createProduct(brand.id)

            testRestTemplate.exchange(
                "$PRODUCTS/${product.id}/likes",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<LikeV1Dto.LikeResponse>>() {},
            )

            val responseType = object : ParameterizedTypeReference<ApiResponse<List<LikeV1Dto.LikeResponse>>>() {}
            val response = testRestTemplate.exchange(
                "$USERS/${user.id}/likes",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).hasSize(1) },
                { assertThat(response.body?.data?.first()?.productId).isEqualTo(product.id) },
            )
        }

        @Test
        @DisplayName("타인의 좋아요 목록 조회 시 403을 반환한다")
        fun getLikedProducts_whenNotOwner_thenReturns403() {
            createUser("testuser")
            val otherUser = createUser("otheruser")

            val responseType = object : ParameterizedTypeReference<ApiResponse<List<LikeV1Dto.LikeResponse>>>() {}
            val response = testRestTemplate.exchange(
                "$USERS/${otherUser.id}/likes",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders("testuser")),
                responseType,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }
    }
}
