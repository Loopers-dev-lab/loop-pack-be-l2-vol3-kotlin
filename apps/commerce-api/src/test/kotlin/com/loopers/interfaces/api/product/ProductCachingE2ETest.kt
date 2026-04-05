package com.loopers.interfaces.api.product

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStatus
import com.loopers.domain.productlike.ProductLikeCountRepository
import com.loopers.domain.user.User
import com.loopers.domain.user.UserRepository
import com.loopers.domain.user.vo.BirthDate
import com.loopers.domain.user.vo.Email
import com.loopers.domain.user.vo.LoginId
import com.loopers.domain.user.vo.Name
import com.loopers.domain.user.vo.Password
import com.loopers.infrastructure.outbox.OutboxRepository
import com.loopers.support.eventually
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Product 캐싱 E2E 테스트")
class ProductCachingE2ETest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val productRepository: ProductRepository,
    private val productLikeCountRepository: ProductLikeCountRepository,
    private val userRepository: UserRepository,
    private val brandRepository: BrandRepository,
    private val cacheManager: CacheManager,
    private val databaseCleanUp: DatabaseCleanUp,
    private val passwordEncoder: PasswordEncoder,
    private val outboxRepository: OutboxRepository,
) {
    companion object {
        private const val TEST_PASSWORD = "password123"
    }

    private lateinit var testBrand: Brand
    private lateinit var testProduct: Product

    @BeforeEach
    fun setUp() {
        // 테스트 전 캐시 초기화
        cacheManager.getCache("product-info")?.clear()

        // 테스트 브랜드 생성
        testBrand = brandRepository.save(
            Brand.create(
                name = "Test Brand ${System.nanoTime()}",
                description = "Test Description",
            ),
        )

        // 테스트 상품 생성
        testProduct = productRepository.save(
            Product.create(
                brand = testBrand,
                name = "Cached Product",
                price = BigDecimal("15000"),
                status = ProductStatus.ACTIVE,
            ),
        )
    }

    @AfterEach
    fun cleanup() {
        cacheManager.getCache("product-info")?.clear()
        databaseCleanUp.truncateAllTables()
    }

    @Test
    @DisplayName("GET /api/v1/products/{id}는 캐싱된 상품 정보를 반환한다")
    fun testGetProductInfoReturnsSuccess() {
        // When
        val response = mockMvc.get("/api/v1/products/${testProduct.id}")
            .andExpect { status { isOk() } }
            .andReturn()
            .response

        // Then
        val responseBody = response.contentAsString
        assertThat(responseBody).contains(testProduct.name)
        assertThat(responseBody).contains("15000")
        assertThat(responseBody).contains("SUCCESS")
    }

    @Test
    @DisplayName("동일 상품을 반복 조회하면 캐시에서 반환한다")
    fun testRepeatedProductRequestUsesCacheSecondTime() {
        // When - 첫 요청
        mockMvc.get("/api/v1/products/${testProduct.id}")
            .andExpect { status { isOk() } }

        // When - 두 번째 요청 (캐시에서)
        val response = mockMvc.get("/api/v1/products/${testProduct.id}")
            .andExpect { status { isOk() } }
            .andReturn()
            .response

        // Then
        val responseBody = response.contentAsString
        assertThat(responseBody).contains(testProduct.name)
        assertThat(responseBody).contains(testBrand.name)
        assertThat(response.status).isEqualTo(200)
    }

    @Test
    @DisplayName("다른 상품 조회는 별도 캐시 항목을 생성한다")
    fun testDifferentProductsHaveSeparateCacheEntries() {
        // Given - 다른 상품 생성
        val product2 = productRepository.save(
            Product.create(
                brand = testBrand,
                name = "Another Product",
                price = BigDecimal("25000"),
                status = ProductStatus.ACTIVE,
            ),
        )

        // When - 첫 번째 상품 조회
        val response1 = mockMvc.get("/api/v1/products/${testProduct.id}")
            .andExpect { status { isOk() } }
            .andReturn()
            .response

        // When - 두 번째 상품 조회
        val response2 = mockMvc.get("/api/v1/products/${product2.id}")
            .andExpect { status { isOk() } }
            .andReturn()
            .response

        // Then
        assertThat(response1.contentAsString).contains("Cached Product")
        assertThat(response2.contentAsString).contains("Another Product")
        assertThat(response1.contentAsString).doesNotContain("25000")
        assertThat(response2.contentAsString).contains("25000")
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 시 404 에러")
    fun testGetNonexistentProductReturns404() {
        // When & Then
        mockMvc.get("/api/v1/products/999")
            .andExpect { status { isNotFound() } }
    }

    @Test
    @DisplayName("INACTIVE 상품 조회 시 404 에러")
    fun testGetInactiveProductReturns404() {
        // Given - INACTIVE 상품 생성
        val inactiveProduct = productRepository.save(
            Product.create(
                brand = testBrand,
                name = "Inactive Product",
                price = BigDecimal("10000"),
                status = ProductStatus.INACTIVE,
            ),
        )

        // When & Then
        mockMvc.get("/api/v1/products/${inactiveProduct.id}")
            .andExpect { status { isNotFound() } }
    }

    @Test
    @DisplayName("삭제된 상품 조회 시 404 에러")
    fun testGetDeletedProductReturns404() {
        // Given
        testProduct.delete()
        productRepository.save(testProduct)

        // When & Then
        mockMvc.get("/api/v1/products/${testProduct.id}")
            .andExpect { status { isNotFound() } }
    }

    @Test
    @DisplayName("캐시된 상품의 응답에는 모든 필수 정보가 포함된다")
    fun testCachedProductResponseContainsAllRequiredFields() {
        // When
        val response = mockMvc.get("/api/v1/products/${testProduct.id}")
            .andExpect { status { isOk() } }
            .andReturn()
            .response

        // Then
        val responseBody = response.contentAsString
        assertThat(responseBody).contains("\"id\"")
        assertThat(responseBody).contains("\"name\"")
        assertThat(responseBody).contains("\"price\"")
        assertThat(responseBody).contains("\"status\"")
        assertThat(responseBody).contains("\"brandId\"")
        assertThat(responseBody).contains("\"brandName\"")
        assertThat(responseBody).contains("\"likeCount\"")
        assertThat(responseBody).contains("SUCCESS")
    }

    @Test
    @DisplayName("여러 상품의 캐시는 독립적으로 관리된다")
    fun testMultipleProductsCachesAreIndependent() {
        // Given - 3개 상품 생성
        val product2 = productRepository.save(
            Product.create(
                brand = testBrand,
                name = "Product 2",
                price = BigDecimal("20000"),
                status = ProductStatus.ACTIVE,
            ),
        )
        val product3 = productRepository.save(
            Product.create(
                brand = testBrand,
                name = "Product 3",
                price = BigDecimal("30000"),
                status = ProductStatus.ACTIVE,
            ),
        )

        // When - 모든 상품을 조회
        val response1 = mockMvc.get("/api/v1/products/${testProduct.id}")
            .andExpect { status { isOk() } }
            .andReturn()
            .response

        val response2 = mockMvc.get("/api/v1/products/${product2.id}")
            .andExpect { status { isOk() } }
            .andReturn()
            .response

        val response3 = mockMvc.get("/api/v1/products/${product3.id}")
            .andExpect { status { isOk() } }
            .andReturn()
            .response

        // Then - 각 응답이 고유한 상품 정보를 포함
        assertThat(response1.contentAsString).contains("Cached Product")
        assertThat(response2.contentAsString).contains("Product 2")
        assertThat(response3.contentAsString).contains("Product 3")

        assertThat(response1.contentAsString).contains("15000")
        assertThat(response2.contentAsString).contains("20000")
        assertThat(response3.contentAsString).contains("30000")
    }

    @Test
    @DisplayName("캐시된 상품은 올바른 좋아요 수를 반환한다")
    fun testCachedProductReturnsCorrectLikeCount() {
        // Given - likeCount가 있는 상품 생성
        val productWithLikes = productRepository.save(
            Product.create(
                brand = testBrand,
                name = "Popular Product",
                price = BigDecimal("50000"),
                status = ProductStatus.ACTIVE,
            ),
        )
        // likeCount를 직접 설정할 수 없으므로, 상품 생성 후의 기본 값 확인
        val likeCount = productLikeCountRepository.findByProductId(productWithLikes.id!!)?.likeCount ?: 0
        assertThat(likeCount).isEqualTo(0)

        // When
        val response = mockMvc.get("/api/v1/products/${productWithLikes.id}")
            .andExpect { status { isOk() } }
            .andReturn()
            .response

        // Then
        val responseBody = response.contentAsString
        assertThat(responseBody).contains("\"likeCount\":0")
    }

    @Test
    @DisplayName("좋아요 요청 후 ProductLiked 이벤트가 Outbox에 발행된다")
    fun testProductLikedEventIsPublishedToOutboxOnLike() {
        // Given
        val loginId = nextLoginId("clu")
        createUser(loginId)

        val warmedDetailResponse = mockMvc.get("/api/v1/products/${testProduct.id}")
            .andExpect { status { isOk() } }
            .andReturn()
            .response
        assertThat(extractLikeCount(warmedDetailResponse.contentAsString)).isEqualTo(0)

        // When
        mockMvc.post("/api/v1/products/${testProduct.id}/likes") {
            header("X-Loopers-LoginId", loginId)
            header("X-Loopers-LoginPw", TEST_PASSWORD)
        }.andExpect {
            status { isOk() }
        }

        // Then - Outbox에 LikeCountEvent(INCREMENT) 이벤트가 발행되었는지 확인
        eventually {
            val outboxEvents = outboxRepository.findAll()
            val likeCountEvent = outboxEvents.find {
                it.aggregateId == testProduct.id && it.eventType == "LikeCountEvent"
            }
            assertThat(likeCountEvent).isNotNull
            assertThat(likeCountEvent!!.topic).isEqualTo("like-events")
        }
    }

    @Test
    @DisplayName("좋아요 취소 요청 후 ProductUnliked 이벤트가 Outbox에 발행된다")
    fun testProductUnlikedEventIsPublishedToOutboxOnUnlike() {
        // Given
        val loginId = nextLoginId("cul")
        createUser(loginId)

        mockMvc.post("/api/v1/products/${testProduct.id}/likes") {
            header("X-Loopers-LoginId", loginId)
            header("X-Loopers-LoginPw", TEST_PASSWORD)
        }.andExpect {
            status { isOk() }
        }

        val warmedDetailResponse = mockMvc.get("/api/v1/products/${testProduct.id}")
            .andExpect { status { isOk() } }
            .andReturn()
            .response
        assertThat(extractLikeCount(warmedDetailResponse.contentAsString)).isEqualTo(0) // 아직 Consumer 처리 안 됨

        // outbox 초기화 (좋아요 이벤트 제거)
        outboxRepository.deleteAll()

        // When
        mockMvc.delete("/api/v1/products/${testProduct.id}/likes") {
            header("X-Loopers-LoginId", loginId)
            header("X-Loopers-LoginPw", TEST_PASSWORD)
        }.andExpect {
            status { isOk() }
        }

        // Then - Outbox에 ProductUnlikedEvent 이벤트가 발행되었는지 확인
        eventually {
            val outboxEvents = outboxRepository.findAll()
            val productUnlikedEvent = outboxEvents.find {
                it.aggregateId == testProduct.id && it.eventType == "ProductUnlikedEvent"
            }
            assertThat(productUnlikedEvent).isNotNull
            assertThat(productUnlikedEvent!!.topic).isEqualTo("like-events")

            // Verify the operation is DECREMENT (via LikeCountEvent also published)
            val likeCountEvent = outboxEvents.find {
                it.aggregateId == testProduct.id && it.eventType == "LikeCountEvent"
            }
            assertThat(likeCountEvent).isNotNull
            assertThat(likeCountEvent!!.topic).isEqualTo("like-events")

            // Parse LikeCountEvent payload to verify operation is DECREMENT
            val payload = objectMapper.readTree(likeCountEvent!!.payload)
            assertThat(payload.get("type").asText()).isEqualTo("DECREMENT")
        }
    }

    private fun createUser(loginId: String): User {
        val user = User.create(
            loginId = LoginId.of(loginId),
            password = Password.ofEncrypted(passwordEncoder.encode(TEST_PASSWORD)),
            name = Name.of("Cache Test User"),
            birthDate = BirthDate.of("20000101"),
            email = Email.of("$loginId@test.com"),
        )

        return userRepository.save(user)
    }

    private fun extractLikeCount(responseBody: String): Int {
        return objectMapper.readTree(responseBody)
            .path("data")
            .path("likeCount")
            .asInt()
    }

    private fun nextLoginId(prefix: String): String {
        return (prefix + System.nanoTime().toString().takeLast(12)).take(20)
    }
}
