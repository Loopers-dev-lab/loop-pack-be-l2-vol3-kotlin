package com.loopers.interfaces.api.user.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.infrastructure.metric.ProductMetricEntity
import com.loopers.infrastructure.metric.ProductMetricJpaRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.page.PageResponse
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
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
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.math.BigDecimal

@DisplayName("GET /api/v1/products - 상품 목록 조회 integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserProductV1ListApiIntegrationTest
@Autowired
constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    private val productMetricJpaRepository: ProductMetricJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {
    companion object {
        private const val ENDPOINT = "/api/v1/products"
        private const val ADMIN = "loopers.admin"
    }

    private var brandId: Long = 0

    @BeforeEach
    fun setUp() {
        val brand = brandRepository.save(Brand.register(name = "나이키"), ADMIN)
        val activeBrand = brand.update("나이키", "ACTIVE")
        val saved = brandRepository.save(activeBrand, ADMIN)
        brandId = saved.id!!
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    private fun createActiveProduct(
        name: String,
        sellingPrice: Long,
        brandId: Long = this.brandId,
    ): Product {
        val product = Product.register(
            name = name,
            regularPrice = Money(BigDecimal.valueOf(sellingPrice)),
            sellingPrice = Money(BigDecimal.valueOf(sellingPrice)),
            brandId = brandId,
        )
        val saved = productRepository.save(product, ADMIN)
        return productRepository.save(saved.activate(), ADMIN)
    }

    private fun putLikeCount(
        productId: Long,
        likeCount: Int,
    ) {
        val current = productMetricJpaRepository.findByProductId(productId)
        val metric = (current ?: ProductMetricEntity(productId = productId))
        metric.likeCount = likeCount
        productMetricJpaRepository.saveAndFlush(metric)
    }

    @Nested
    @DisplayName("상품 목록 조회 시")
    inner class WhenGetList {
        @Test
        @DisplayName("ACTIVE 상태의 상품만 조회된다")
        fun getList_onlyActive() {
            createActiveProduct("활성 상품", 10000)
            val inactive = Product.register(
                name = "비활성 상품",
                regularPrice = Money(BigDecimal.valueOf(5000)),
                sellingPrice = Money(BigDecimal.valueOf(5000)),
                brandId = brandId,
            )
            productRepository.save(inactive, ADMIN)

            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<PageResponse<UserProductV1Response.Summary>>>() {},
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body?.data?.content).hasSize(1)
            assertThat(response.body?.data?.content?.first()?.name).isEqualTo("활성 상품")
        }

        @Test
        @DisplayName("PRICE_ASC 정렬로 조회할 수 있다")
        fun getList_sortByPriceAsc() {
            createActiveProduct("비싼상품", 20000)
            createActiveProduct("싼상품", 5000)

            val response = testRestTemplate.exchange(
                "$ENDPOINT?sort=PRICE_ASC",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<PageResponse<UserProductV1Response.Summary>>>() {},
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val content = response.body?.data?.content!!
            assertThat(content).hasSize(2)
            assertThat(content[0].name).isEqualTo("싼상품")
            assertThat(content[1].name).isEqualTo("비싼상품")
        }

        @Test
        @DisplayName("PRICE_ASC 동률일 때 id DESC 로 안정적으로 페이지네이션된다")
        fun getList_sortByPriceAsc_stableAcrossPages() {
            val products = (1..11).map { index ->
                createActiveProduct("동일가격상품$index", 10000)
            }
            val expectedIds = products.map { it.id!! }.sortedDescending()

            val firstPage = testRestTemplate.exchange(
                "$ENDPOINT?brandId=$brandId&sort=PRICE_ASC&page=0&size=10",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<PageResponse<UserProductV1Response.Summary>>>() {},
            )
            val secondPage = testRestTemplate.exchange(
                "$ENDPOINT?brandId=$brandId&sort=PRICE_ASC&page=1&size=10",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<PageResponse<UserProductV1Response.Summary>>>() {},
            )

            assertThat(firstPage.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(secondPage.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(firstPage.body?.data?.content?.map { it.id }).containsExactlyElementsOf(expectedIds.take(10))
            assertThat(secondPage.body?.data?.content?.map { it.id }).containsExactlyElementsOf(expectedIds.drop(10))
        }

        @Test
        @DisplayName("LIKES_DESC 정렬로 조회할 수 있다")
        fun getList_sortByLikesDesc() {
            val lowLikes = createActiveProduct("낮은좋아요", 10000)
            val highLikes = createActiveProduct("높은좋아요", 10000)
            putLikeCount(lowLikes.id!!, 1)
            putLikeCount(highLikes.id!!, 3)

            val response = testRestTemplate.exchange(
                "$ENDPOINT?sort=LIKES_DESC",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<PageResponse<UserProductV1Response.Summary>>>() {},
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val content = response.body?.data?.content!!
            assertThat(content).hasSize(2)
            assertThat(content[0].name).isEqualTo("높은좋아요")
            assertThat(content[1].name).isEqualTo("낮은좋아요")
        }

        @Test
        @DisplayName("LIKES_DESC 동률일 때 id DESC 로 안정적으로 페이지네이션된다")
        fun getList_sortByLikesDesc_stableAcrossPages() {
            val products = (1..11).map { index ->
                createActiveProduct("동일좋아요상품$index", 10000)
            }
            products.forEach { product ->
                putLikeCount(product.id!!, 3)
            }
            val expectedIds = products.map { it.id!! }.sortedDescending()

            val firstPage = testRestTemplate.exchange(
                "$ENDPOINT?brandId=$brandId&sort=LIKES_DESC&page=0&size=10",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<PageResponse<UserProductV1Response.Summary>>>() {},
            )
            val secondPage = testRestTemplate.exchange(
                "$ENDPOINT?brandId=$brandId&sort=LIKES_DESC&page=1&size=10",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<PageResponse<UserProductV1Response.Summary>>>() {},
            )

            assertThat(firstPage.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(secondPage.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(firstPage.body?.data?.content?.map { it.id }).containsExactlyElementsOf(expectedIds.take(10))
            assertThat(secondPage.body?.data?.content?.map { it.id }).containsExactlyElementsOf(expectedIds.drop(10))
        }

        @Test
        @DisplayName("brandId 필터가 없어도 LIKES_DESC 는 active 브랜드 전체에서 안정적으로 페이지네이션된다")
        fun getList_sortByLikesDesc_withoutBrandFilter_stableAcrossPages() {
            val brand2 = brandRepository.save(Brand.register(name = "아디다스"), ADMIN)
            val activeBrand2 = brandRepository.save(brand2.update("아디다스", "ACTIVE"), ADMIN)
            val products = (1..6).map { index ->
                createActiveProduct("나이키좋아요상품$index", 10000)
            } + (1..5).map { index ->
                createActiveProduct("아디다스좋아요상품$index", 10000, activeBrand2.id!!)
            }
            products.forEach { product ->
                putLikeCount(product.id!!, 3)
            }
            val expectedIds = products.map { it.id!! }.sortedDescending()

            val firstPage = testRestTemplate.exchange(
                "$ENDPOINT?sort=LIKES_DESC&page=0&size=10",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<PageResponse<UserProductV1Response.Summary>>>() {},
            )
            val secondPage = testRestTemplate.exchange(
                "$ENDPOINT?sort=LIKES_DESC&page=1&size=10",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<PageResponse<UserProductV1Response.Summary>>>() {},
            )

            assertThat(firstPage.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(secondPage.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(firstPage.body?.data?.content?.map { it.id }).containsExactlyElementsOf(expectedIds.take(10))
            assertThat(secondPage.body?.data?.content?.map { it.id }).containsExactlyElementsOf(expectedIds.drop(10))
        }
    }
}
