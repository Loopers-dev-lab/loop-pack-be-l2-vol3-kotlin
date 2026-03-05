package com.loopers.interfaces.api.user.like

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.user.User
import com.loopers.domain.user.UserPasswordHasher
import com.loopers.domain.user.UserRepository
import com.loopers.interfaces.api.ApiResponse
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
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("DELETE /api/v1/products/{productId}/likes - 상품 좋아요 취소 E2E")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductLikeV1CancelE2ETest
@Autowired
constructor(
    private val testRestTemplate: TestRestTemplate,
    private val userRepository: UserRepository,
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    private val passwordHasher: UserPasswordHasher,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ADMIN = "loopers.admin"
        private const val LOGIN_ID = "testuser1"
        private const val PASSWORD = "Password1!"
    }

    private var productId: Long = 0

    @BeforeEach
    fun setUp() {
        val user = User.register(
            loginId = LOGIN_ID,
            rawPassword = PASSWORD,
            name = "홍길동",
            birthDate = LocalDate.of(1990, 1, 1),
            email = "test@example.com",
            passwordHasher = passwordHasher,
        )
        userRepository.save(user)

        val brand = brandRepository.save(Brand.register(name = "나이키"), ADMIN)
        val activeBrand = brandRepository.save(brand.update("나이키", "ACTIVE"), ADMIN)

        val product = Product.register(
            name = "테스트 상품",
            regularPrice = Money(BigDecimal.valueOf(10000)),
            sellingPrice = Money(BigDecimal.valueOf(10000)),
            brandId = activeBrand.id!!,
        )
        val saved = productRepository.save(product, ADMIN)
        productId = productRepository.save(saved.activate(), ADMIN).id!!
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun endpoint(productId: Long): String = "/api/v1/products/$productId/likes"

    private fun authHeaders(loginId: String = LOGIN_ID, password: String = PASSWORD): HttpEntity<Unit> {
        val headers = HttpHeaders().apply {
            set("X-Loopers-LoginId", loginId)
            set("X-Loopers-LoginPw", password)
        }
        return HttpEntity(Unit, headers)
    }

    private fun registerLike(productId: Long) {
        testRestTemplate.exchange(
            endpoint(productId),
            HttpMethod.POST,
            authHeaders(),
            object : ParameterizedTypeReference<ApiResponse<Nothing?>>() {},
        )
    }

    @Nested
    @DisplayName("좋아요 취소 성공 시")
    inner class WhenCancelSuccess {
        @Test
        @DisplayName("등록된 좋아요를 취소하면 200 OK를 반환한다")
        fun cancel_success_returns200() {
            // arrange
            registerLike(productId)

            // act
            val response = testRestTemplate.exchange(
                endpoint(productId),
                HttpMethod.DELETE,
                authHeaders(),
                object : ParameterizedTypeReference<ApiResponse<Nothing?>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body?.meta?.result?.name).isEqualTo("SUCCESS")
        }

        @Test
        @DisplayName("등록하지 않은 좋아요를 취소하면 200 OK를 반환한다 (멱등적)")
        fun cancel_notRegistered_returns200() {
            // act
            val response = testRestTemplate.exchange(
                endpoint(productId),
                HttpMethod.DELETE,
                authHeaders(),
                object : ParameterizedTypeReference<ApiResponse<Nothing?>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }
    }

    @Nested
    @DisplayName("좋아요 취소 실패 시")
    inner class WhenCancelFails {
        @Test
        @DisplayName("인증 실패 시 401을 반환한다")
        fun cancel_unauthorized_returns401() {
            // act
            val response = testRestTemplate.exchange(
                endpoint(productId),
                HttpMethod.DELETE,
                authHeaders(password = "WrongPassword1!"),
                object : ParameterizedTypeReference<ApiResponse<Any?>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }
}
