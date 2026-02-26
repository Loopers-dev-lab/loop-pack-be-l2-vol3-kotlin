package com.loopers.interfaces.api

import com.loopers.domain.brand.Brand
import com.loopers.interfaces.common.ApiResponse
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.LikeCount
import com.loopers.domain.common.Money
import com.loopers.domain.common.StockQuantity
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.interfaces.api.like.LikeDto
import com.loopers.interfaces.api.user.UserDto
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
import org.springframework.http.MediaType
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserLikeApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val USER_LIKES_ENDPOINT = "/api/v1/users/{userId}/likes"
        private const val LIKE_ENDPOINT = "/api/v1/products/{productId}/likes"
        private const val SIGNUP_ENDPOINT = "/api/v1/users/signup"
        private const val LOGIN_ID_HEADER = "X-Loopers-LoginId"
        private const val LOGIN_PW_HEADER = "X-Loopers-LoginPw"
        private val USER_LIKES_RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<List<LikeDto.UserLikeResponse>>>() {}
        private val ERROR_RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<Any>>() {}
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun signUpAndGetUserId(
        loginId: String = "testuser123",
        password: String = "Test1234!@",
        name: String = "홍길동",
        email: String = "test@example.com",
        birthday: LocalDate = LocalDate.of(1990, 1, 15),
    ): Long {
        val request = UserDto.SignUpRequest(
            loginId = loginId,
            password = password,
            name = name,
            email = email,
            birthday = birthday,
        )
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }
        val response = testRestTemplate.exchange(
            SIGNUP_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(request, headers),
            object : ParameterizedTypeReference<ApiResponse<UserDto.SignUpResponse>>() {},
        )
        return response.body!!.data!!.id
    }

    private fun authHeaders(
        loginId: String = "testuser123",
        password: String = "Test1234!@",
    ): HttpHeaders {
        return HttpHeaders().apply {
            set(LOGIN_ID_HEADER, loginId)
            set(LOGIN_PW_HEADER, password)
        }
    }

    private fun createProduct(
        name: String = "에어맥스",
        description: String? = "러닝화",
        price: Money = Money.of(159000L),
        likes: LikeCount = LikeCount.of(0),
        stockQuantity: StockQuantity = StockQuantity.of(100),
    ): Product {
        val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
        return productRepository.save(
            Product(name = name, description = description, price = price, likes = likes, stockQuantity = stockQuantity, brandId = brand.id),
        )
    }

    private fun likeProduct(productId: Long) {
        testRestTemplate.exchange(
            LIKE_ENDPOINT,
            HttpMethod.POST,
            HttpEntity<Void>(authHeaders()),
            object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            productId,
        )
    }

    @DisplayName("GET /api/v1/users/{userId}/likes")
    @Nested
    inner class GetUserLikes {

        @DisplayName("로그인한 사용자가 자신의 좋아요 목록을 조회하면, 200 OK와 상품 목록을 반환한다.")
        @Test
        fun returnsLikedProducts_whenAuthenticatedUserRequestsOwnLikes() {
            // arrange
            val userId = signUpAndGetUserId()
            val product1 = createProduct(name = "에어맥스", price = Money.of(159000L))
            val product2 = createProduct(name = "에어포스", price = Money.of(129000L))
            likeProduct(product1.id)
            likeProduct(product2.id)

            // act
            val response = testRestTemplate.exchange(
                USER_LIKES_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                USER_LIKES_RESPONSE_TYPE,
                userId,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).hasSize(2) },
            )
        }

        @DisplayName("좋아요한 상품이 없으면, 빈 목록을 반환한다.")
        @Test
        fun returnsEmptyList_whenNoLikes() {
            // arrange
            val userId = signUpAndGetUserId()

            // act
            val response = testRestTemplate.exchange(
                USER_LIKES_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                USER_LIKES_RESPONSE_TYPE,
                userId,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isEmpty() },
            )
        }

        @DisplayName("삭제된 상품은 목록에서 제외된다.")
        @Test
        fun excludesDeletedProducts() {
            // arrange
            val userId = signUpAndGetUserId()
            val product1 = createProduct(name = "에어맥스", price = Money.of(159000L))
            val product2 = createProduct(name = "단종상품", price = Money.of(99000L))
            likeProduct(product1.id)
            likeProduct(product2.id)
            product2.delete()
            productRepository.save(product2)

            // act
            val response = testRestTemplate.exchange(
                USER_LIKES_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                USER_LIKES_RESPONSE_TYPE,
                userId,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).hasSize(1) },
            )
        }

        @DisplayName("인증 헤더가 없으면, 401 Unauthorized를 반환한다.")
        @Test
        fun returnsUnauthorized_whenNotAuthenticated() {
            // arrange & act
            val response = testRestTemplate.exchange(
                USER_LIKES_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Void>(HttpHeaders()),
                ERROR_RESPONSE_TYPE,
                1L,
            )

            // assert
            assertThat(response.statusCode.value()).isEqualTo(401)
        }

        @DisplayName("다른 사용자의 좋아요 목록을 조회하면, 403 Forbidden을 반환한다.")
        @Test
        fun returnsForbidden_whenAccessingOtherUserLikes() {
            // arrange
            signUpAndGetUserId()

            // act - 자신의 ID가 아닌 다른 userId로 요청
            val response = testRestTemplate.exchange(
                USER_LIKES_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                ERROR_RESPONSE_TYPE,
                999999L,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }
    }
}
