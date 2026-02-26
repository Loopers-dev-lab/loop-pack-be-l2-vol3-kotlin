package com.loopers.interfaces.api

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.common.StockQuantity
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.interfaces.api.product.ProductDto
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
class LikeApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val LIKE_ENDPOINT = "/api/v1/products/{productId}/likes"
        private const val PRODUCT_DETAIL_ENDPOINT = "/api/v1/products/{productId}"
        private const val SIGNUP_ENDPOINT = "/api/v1/users/signup"
        private const val LOGIN_ID_HEADER = "X-Loopers-LoginId"
        private const val LOGIN_PW_HEADER = "X-Loopers-LoginPw"
        private val LIKE_RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<Any>>() {}
        private val DETAIL_RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<ProductDto.DetailResponse>>() {}
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun signUp(
        loginId: String = "testuser123",
        password: String = "Test1234!@",
        name: String = "홍길동",
        email: String = "test@example.com",
        birthday: LocalDate = LocalDate.of(1990, 1, 15),
    ) {
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
        testRestTemplate.exchange(
            SIGNUP_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(request, headers),
            object : ParameterizedTypeReference<ApiResponse<UserDto.SignUpResponse>>() {},
        )
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
        likes: Int = 0,
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
            LIKE_RESPONSE_TYPE,
            productId,
        )
    }

    private fun unlikeProduct(productId: Long) {
        testRestTemplate.exchange(
            LIKE_ENDPOINT,
            HttpMethod.DELETE,
            HttpEntity<Void>(authHeaders()),
            LIKE_RESPONSE_TYPE,
            productId,
        )
    }

    private fun getLikeCount(productId: Long): Int? {
        val detailResponse = testRestTemplate.exchange(
            PRODUCT_DETAIL_ENDPOINT,
            HttpMethod.GET,
            HttpEntity<Void>(HttpHeaders()),
            DETAIL_RESPONSE_TYPE,
            productId,
        )
        return detailResponse.body?.data?.likeCount
    }

    @DisplayName("POST /api/v1/products/{productId}/likes")
    @Nested
    inner class LikeProduct {

        @DisplayName("로그인한 사용자가 좋아요를 누르면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenAuthenticatedUserLikesProduct() {
            // arrange
            signUp()
            val product = createProduct()
            val httpEntity = HttpEntity<Void>(authHeaders())

            // act
            val response = testRestTemplate.exchange(
                LIKE_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                LIKE_RESPONSE_TYPE,
                product.id,
            )

            // assert
            assertThat(response.statusCode.is2xxSuccessful).isTrue()
        }

        @DisplayName("이미 좋아요한 상품에 다시 좋아요하면, 200 OK를 반환한다. (멱등)")
        @Test
        fun returnsOk_whenAlreadyLiked() {
            // arrange
            signUp()
            val product = createProduct()
            likeProduct(product.id)

            // act - 두 번째 좋아요 (멱등)
            val response = testRestTemplate.exchange(
                LIKE_ENDPOINT,
                HttpMethod.POST,
                HttpEntity<Void>(authHeaders()),
                LIKE_RESPONSE_TYPE,
                product.id,
            )

            // assert
            assertThat(response.statusCode.is2xxSuccessful).isTrue()
        }

        @DisplayName("인증 헤더가 없으면, 401 Unauthorized를 반환한다.")
        @Test
        fun returnsUnauthorized_whenNotAuthenticated() {
            // arrange
            val product = createProduct()
            val httpEntity = HttpEntity<Void>(HttpHeaders())

            // act
            val response = testRestTemplate.exchange(
                LIKE_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                LIKE_RESPONSE_TYPE,
                product.id,
            )

            // assert
            assertThat(response.statusCode.value()).isEqualTo(401)
        }

        @DisplayName("존재하지 않는 상품에 좋아요하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenProductNotExists() {
            // arrange
            signUp()
            val httpEntity = HttpEntity<Void>(authHeaders())

            // act
            val response = testRestTemplate.exchange(
                LIKE_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                LIKE_RESPONSE_TYPE,
                999999L,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("좋아요를 누르면, 상품의 좋아요 수가 1 증가한다.")
        @Test
        fun increasesLikeCount_whenUserLikesProduct() {
            // arrange
            signUp()
            val product = createProduct()

            // act
            likeProduct(product.id)

            // assert
            assertThat(getLikeCount(product.id)).isEqualTo(1)
        }

        @DisplayName("이미 좋아요한 상품에 다시 좋아요하면, 좋아요 수가 변경되지 않는다. (멱등)")
        @Test
        fun doesNotChangeLikeCount_whenAlreadyLiked() {
            // arrange
            signUp()
            val product = createProduct()
            likeProduct(product.id)

            // act - 두 번째 좋아요 (멱등)
            likeProduct(product.id)

            // assert
            assertThat(getLikeCount(product.id)).isEqualTo(1)
        }

        @DisplayName("삭제된 상품에 좋아요하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenProductIsDeleted() {
            // arrange
            signUp()
            val product = createProduct(name = "단종상품", description = "단종", price = Money.of(99000L), stockQuantity = StockQuantity.of(0))
            product.delete()
            productRepository.save(product)
            val httpEntity = HttpEntity<Void>(authHeaders())

            // act
            val response = testRestTemplate.exchange(
                LIKE_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                LIKE_RESPONSE_TYPE,
                product.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }
    }

    @DisplayName("DELETE /api/v1/products/{productId}/likes")
    @Nested
    inner class UnlikeProduct {

        @DisplayName("로그인한 사용자가 좋아요를 취소하면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenAuthenticatedUserUnlikesProduct() {
            // arrange
            signUp()
            val product = createProduct()
            likeProduct(product.id)

            // act - 좋아요 취소
            val response = testRestTemplate.exchange(
                LIKE_ENDPOINT,
                HttpMethod.DELETE,
                HttpEntity<Void>(authHeaders()),
                LIKE_RESPONSE_TYPE,
                product.id,
            )

            // assert
            assertThat(response.statusCode.is2xxSuccessful).isTrue()
        }

        @DisplayName("좋아요하지 않은 상품을 취소하면, 200 OK를 반환한다. (멱등)")
        @Test
        fun returnsOk_whenNotLiked() {
            // arrange
            signUp()
            val product = createProduct()
            val httpEntity = HttpEntity<Void>(authHeaders())

            // act - 좋아요하지 않은 상태에서 취소 (멱등)
            val response = testRestTemplate.exchange(
                LIKE_ENDPOINT,
                HttpMethod.DELETE,
                httpEntity,
                LIKE_RESPONSE_TYPE,
                product.id,
            )

            // assert
            assertThat(response.statusCode.is2xxSuccessful).isTrue()
        }

        @DisplayName("인증 헤더가 없으면, 401 Unauthorized를 반환한다.")
        @Test
        fun returnsUnauthorized_whenNotAuthenticated() {
            // arrange
            val product = createProduct()
            val httpEntity = HttpEntity<Void>(HttpHeaders())

            // act
            val response = testRestTemplate.exchange(
                LIKE_ENDPOINT,
                HttpMethod.DELETE,
                httpEntity,
                LIKE_RESPONSE_TYPE,
                product.id,
            )

            // assert
            assertThat(response.statusCode.value()).isEqualTo(401)
        }

        @DisplayName("존재하지 않는 상품에 좋아요 취소하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenProductNotExists() {
            // arrange
            signUp()
            val httpEntity = HttpEntity<Void>(authHeaders())

            // act
            val response = testRestTemplate.exchange(
                LIKE_ENDPOINT,
                HttpMethod.DELETE,
                httpEntity,
                LIKE_RESPONSE_TYPE,
                999999L,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("좋아요 취소하면, 상품의 좋아요 수가 1 감소한다.")
        @Test
        fun decreasesLikeCount_whenUserUnlikesProduct() {
            // arrange
            signUp()
            val product = createProduct()
            likeProduct(product.id)

            // act
            unlikeProduct(product.id)

            // assert
            assertThat(getLikeCount(product.id)).isEqualTo(0)
        }

        @DisplayName("좋아요하지 않은 상품을 취소하면, 좋아요 수가 변경되지 않는다. (멱등)")
        @Test
        fun doesNotChangeLikeCount_whenNotLiked() {
            // arrange
            signUp()
            val product = createProduct()

            // act
            unlikeProduct(product.id)

            // assert
            assertThat(getLikeCount(product.id)).isEqualTo(0)
        }

        @DisplayName("삭제된 상품에 좋아요 취소하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenProductIsDeleted() {
            // arrange
            signUp()
            val product = createProduct(name = "단종상품", description = "단종", price = Money.of(99000L), stockQuantity = StockQuantity.of(0))
            product.delete()
            productRepository.save(product)
            val httpEntity = HttpEntity<Void>(authHeaders())

            // act
            val response = testRestTemplate.exchange(
                LIKE_ENDPOINT,
                HttpMethod.DELETE,
                httpEntity,
                LIKE_RESPONSE_TYPE,
                product.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }
    }
}
