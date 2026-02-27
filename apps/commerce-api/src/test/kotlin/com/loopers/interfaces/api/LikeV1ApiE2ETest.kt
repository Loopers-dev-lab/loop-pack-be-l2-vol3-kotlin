package com.loopers.interfaces.api

import com.loopers.domain.brand.Brand
import com.loopers.domain.like.ProductLike
import com.loopers.domain.product.Product
import com.loopers.domain.user.User
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.like.ProductLikeJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.interfaces.api.like.LikeV1Dto
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
class LikeV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val userJpaRepository: UserJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val productLikeJpaRepository: ProductLikeJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    private lateinit var userHeaders: HttpHeaders
    private lateinit var user: User
    private lateinit var product: Product

    companion object {
        private const val PASSWORD = "abcd1234"
    }

    @BeforeEach
    fun setUp() {
        user = userJpaRepository.save(User(loginId = "testuser1", password = PASSWORD, name = "테스트유저", birth = "2000-01-01", email = "test@test.com"))
        val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
        product = productJpaRepository.save(Product(brandId = brand.id, name = "에어맥스", description = "운동화", price = 139000, stockQuantity = 100))
        userHeaders = HttpHeaders()
        userHeaders.set("X-Loopers-LoginId", "testuser1")
        userHeaders.set("X-Loopers-LoginPw", PASSWORD)
    }

    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("POST /api/v1/products/{productId}/likes")
    @Nested
    inner class Like {
        @DisplayName("인증된 유저가 좋아요를 등록하면, 성공 응답을 반환한다.")
        @Test
        fun returnsSuccess_whenLikeIsCreated() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange("/api/v1/products/${product.id}/likes", HttpMethod.POST, HttpEntity<Any>(Unit, userHeaders), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(productLikeJpaRepository.findByUserIdAndProductId(user.id, product.id)).isNotNull() },
            )
        }

        @DisplayName("인증되지 않은 요청은 404 응답을 받는다.")
        @Test
        fun returnsNotFound_whenNotAuthenticated() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val headers = HttpHeaders()
            headers.set("X-Loopers-LoginId", "wronguser")
            headers.set("X-Loopers-LoginPw", "wrongpass1")
            val response = testRestTemplate.exchange("/api/v1/products/${product.id}/likes", HttpMethod.POST, HttpEntity<Any>(Unit, headers), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("DELETE /api/v1/products/{productId}/likes")
    @Nested
    inner class Unlike {
        @DisplayName("인증된 유저가 좋아요를 취소하면, 성공 응답을 반환한다.")
        @Test
        fun returnsSuccess_whenLikeIsDeleted() {
            // arrange
            productLikeJpaRepository.save(ProductLike(userId = user.id, productId = product.id))

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange("/api/v1/products/${product.id}/likes", HttpMethod.DELETE, HttpEntity<Any>(Unit, userHeaders), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(productLikeJpaRepository.findByUserIdAndProductId(user.id, product.id)).isNull() },
            )
        }
    }

    @DisplayName("GET /api/v1/likes")
    @Nested
    inner class GetUserLikes {
        @DisplayName("인증된 유저의 좋아요 목록을 반환한다.")
        @Test
        fun returnsLikeList_whenUserHasLikes() {
            // arrange
            productLikeJpaRepository.save(ProductLike(userId = user.id, productId = product.id))

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<List<LikeV1Dto.LikeProductResponse>>>() {}
            val response = testRestTemplate.exchange("/api/v1/likes", HttpMethod.GET, HttpEntity<Any>(Unit, userHeaders), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data).hasSize(1) },
                { assertThat(response.body?.data?.get(0)?.productId).isEqualTo(product.id) },
                { assertThat(response.body?.data?.get(0)?.productName).isEqualTo("에어맥스") },
            )
        }
    }
}
