package com.loopers.interfaces.api.productlike

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductStatus
import com.loopers.domain.productlike.dto.LikedProductInfo
import com.loopers.domain.user.User
import com.loopers.domain.user.vo.BirthDate
import com.loopers.domain.user.vo.Email
import com.loopers.domain.user.vo.LoginId
import com.loopers.domain.user.vo.Name
import com.loopers.domain.user.vo.Password
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
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
import org.springframework.security.crypto.password.PasswordEncoder
import java.math.BigDecimal

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("ProductLike V1 API E2E Test")
class ProductLikeV1ControllerE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val passwordEncoder: PasswordEncoder,
) {

    companion object {
        private const val API_ENDPOINT = "/api/v1/products"
        private const val USERS_ENDPOINT = "/api/v1/users"
        private const val PLAIN_PASSWORD = "password123"

        private fun createAuthHeaders(loginId: String): HttpHeaders {
            val headers = HttpHeaders()
            headers["X-Loopers-LoginId"] = loginId
            headers["X-Loopers-LoginPw"] = PLAIN_PASSWORD
            return headers
        }
    }

    @AfterEach
    fun cleanup() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    @DisplayName("좋아요 추가")
    inner class LikeProductTest {

        @Test
        @DisplayName("상품을 좋아요 할 수 있다")
        fun likeProduct() {
            // Arrange
            val brand = brandJpaRepository.save(
                Brand.create(name = "Test Brand", description = "Test Brand Description"),
            )
            val product = productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "Test Product",
                    price = BigDecimal("10000"),
                    stock = 100,
                    status = ProductStatus.ACTIVE,
                ),
            )
            val user = userJpaRepository.save(
                User.create(
                    loginId = LoginId.of("testuser"),
                    password = Password.ofEncrypted(passwordEncoder.encode(PLAIN_PASSWORD)),
                    name = Name.of("Test User"),
                    birthDate = BirthDate.of("20000101"),
                    email = Email.of("test@example.com"),
                ),
            )

            // Act
            val response = testRestTemplate.exchange(
                "$API_ENDPOINT/${product.id}/likes",
                HttpMethod.POST,
                HttpEntity(null, createAuthHeaders("testuser")),
                ApiResponse::class.java,
            )

            // Assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }
    }

    @Nested
    @DisplayName("좋아요 취소")
    inner class UnlikeProductTest {

        @Test
        @DisplayName("상품의 좋아요를 취소할 수 있다")
        fun unlikeProduct() {
            // Arrange
            val brand = brandJpaRepository.save(
                Brand.create(name = "Test Brand", description = "Test Brand Description"),
            )
            val product = productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "Test Product",
                    price = BigDecimal("10000"),
                    stock = 100,
                    status = ProductStatus.ACTIVE,
                ),
            )
            val user = userJpaRepository.save(
                User.create(
                    loginId = LoginId.of("testuser"),
                    password = Password.ofEncrypted(passwordEncoder.encode(PLAIN_PASSWORD)),
                    name = Name.of("Test User"),
                    birthDate = BirthDate.of("20000101"),
                    email = Email.of("test@example.com"),
                ),
            )

            // 먼저 좋아요 추가
            testRestTemplate.exchange(
                "$API_ENDPOINT/${product.id}/likes",
                HttpMethod.POST,
                HttpEntity(null, createAuthHeaders("testuser")),
                ApiResponse::class.java,
            )

            // Act
            val response = testRestTemplate.exchange(
                "$API_ENDPOINT/${product.id}/likes",
                HttpMethod.DELETE,
                HttpEntity(null, createAuthHeaders("testuser")),
                ApiResponse::class.java,
            )

            // Assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }
    }

    @Nested
    @DisplayName("에러 처리")
    inner class ErrorHandlingTest {

        @Test
        @DisplayName("존재하지 않는 상품에 좋아요하면 404를 받는다")
        fun likeNonExistentProduct() {
            // Arrange
            val user = userJpaRepository.save(
                User.create(
                    loginId = LoginId.of("testuser"),
                    password = Password.ofEncrypted(passwordEncoder.encode(PLAIN_PASSWORD)),
                    name = Name.of("Test User"),
                    birthDate = BirthDate.of("20000101"),
                    email = Email.of("test@example.com"),
                ),
            )

            // Act
            val response = testRestTemplate.exchange(
                "$API_ENDPOINT/999999/likes",
                HttpMethod.POST,
                HttpEntity(null, createAuthHeaders("testuser")),
                ApiResponse::class.java,
            )

            // Assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("내 좋아요 목록 조회")
    inner class GetMyLikedProductsTest {

        @Test
        @DisplayName("내가 좋아한 상품 목록을 조회할 수 있다")
        fun getMyLikedProducts() {
            // Arrange
            val brand = brandJpaRepository.save(
                Brand.create(name = "Test Brand", description = "Test Brand Description"),
            )
            val product1 = productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "Product 1",
                    price = BigDecimal("10000"),
                    stock = 100,
                    status = ProductStatus.ACTIVE,
                ),
            )
            val product2 = productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "Product 2",
                    price = BigDecimal("20000"),
                    stock = 50,
                    status = ProductStatus.ACTIVE,
                ),
            )
            val user = userJpaRepository.save(
                User.create(
                    loginId = LoginId.of("testuser"),
                    password = Password.ofEncrypted(passwordEncoder.encode(PLAIN_PASSWORD)),
                    name = Name.of("Test User"),
                    birthDate = BirthDate.of("20000101"),
                    email = Email.of("test@example.com"),
                ),
            )

            // 두 상품에 좋아요 추가
            testRestTemplate.exchange(
                "$API_ENDPOINT/${product1.id}/likes",
                HttpMethod.POST,
                HttpEntity(null, createAuthHeaders("testuser")),
                ApiResponse::class.java,
            )
            testRestTemplate.exchange(
                "$API_ENDPOINT/${product2.id}/likes",
                HttpMethod.POST,
                HttpEntity(null, createAuthHeaders("testuser")),
                ApiResponse::class.java,
            )

            // Act
            val response = testRestTemplate.exchange(
                "$USERS_ENDPOINT/${user.id}/likes?page=0&size=20",
                HttpMethod.GET,
                HttpEntity(null, createAuthHeaders("testuser")),
                object : ParameterizedTypeReference<ApiResponse<PageResponse<LikedProductInfo>>>() {},
            )

            // Assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.content).hasSize(2) },
                { assertThat(response.body?.data?.content?.map { it.id }).containsExactlyInAnyOrder(product1.id, product2.id) },
            )
        }
    }
}
