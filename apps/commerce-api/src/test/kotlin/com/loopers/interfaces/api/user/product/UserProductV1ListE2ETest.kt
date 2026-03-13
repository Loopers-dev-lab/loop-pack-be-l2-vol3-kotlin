package com.loopers.interfaces.api.user.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.page.PageResponse
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
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.math.BigDecimal

@DisplayName("GET /api/v1/products - 상품 목록 조회 E2E")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserProductV1ListE2ETest
@Autowired
constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    private val databaseCleanUp: DatabaseCleanUp,
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

    @Nested
    @DisplayName("상품 목록 조회 시")
    inner class WhenGetList {
        @Test
        @DisplayName("ACTIVE 상태의 상품만 조회된다")
        fun getList_onlyActive() {
            // arrange
            createActiveProduct("활성 상품", 10000)
            val inactive = Product.register(
                name = "비활성 상품",
                regularPrice = Money(BigDecimal.valueOf(5000)),
                sellingPrice = Money(BigDecimal.valueOf(5000)),
                brandId = brandId,
            )
            productRepository.save(inactive, ADMIN)

            // act
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<PageResponse<UserProductV1Response.Summary>>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body?.data?.content).hasSize(1)
            assertThat(response.body?.data?.content?.first()?.name).isEqualTo("활성 상품")
        }

        @Test
        @DisplayName("PRICE_ASC 정렬로 조회할 수 있다")
        fun getList_sortByPriceAsc() {
            // arrange
            createActiveProduct("비싼상품", 20000)
            createActiveProduct("싼상품", 5000)

            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT?sort=PRICE_ASC",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<PageResponse<UserProductV1Response.Summary>>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val content = response.body?.data?.content!!
            assertThat(content).hasSize(2)
            assertThat(content[0].name).isEqualTo("싼상품")
            assertThat(content[1].name).isEqualTo("비싼상품")
        }

        @Test
        @DisplayName("PRICE_ASC 동률일 때 id DESC 로 안정적으로 페이지네이션된다")
        fun getList_sortByPriceAsc_stableAcrossPages() {
            // arrange
            val products = (1..11).map { index ->
                createActiveProduct("동일가격상품$index", 10000)
            }
            val expectedIds = products.map { it.id!! }.sortedDescending()

            // act
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

            // assert
            assertThat(firstPage.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(secondPage.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(firstPage.body?.data?.content?.map { it.id }).containsExactlyElementsOf(expectedIds.take(10))
            assertThat(secondPage.body?.data?.content?.map { it.id }).containsExactlyElementsOf(expectedIds.drop(10))
        }

        @Test
        @DisplayName("LIKES_DESC 정렬로 조회할 수 있다")
        fun getList_sortByLikesDesc() {
            // arrange
            val lowLikes = createActiveProduct("낮은좋아요", 10000)
            val highLikes = createActiveProduct("높은좋아요", 10000)
            productRepository.incrementLikeCount(lowLikes.id!!)
            repeat(3) {
                productRepository.incrementLikeCount(highLikes.id!!)
            }

            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT?sort=LIKES_DESC",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<PageResponse<UserProductV1Response.Summary>>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val content = response.body?.data?.content!!
            assertThat(content).hasSize(2)
            assertThat(content[0].name).isEqualTo("높은좋아요")
            assertThat(content[1].name).isEqualTo("낮은좋아요")
        }

        @Test
        @DisplayName("LIKES_DESC 동률일 때 id DESC 로 안정적으로 페이지네이션된다")
        fun getList_sortByLikesDesc_stableAcrossPages() {
            // arrange
            val products = (1..11).map { index ->
                createActiveProduct("동일좋아요상품$index", 10000)
            }
            products.forEach { product ->
                repeat(3) {
                    productRepository.incrementLikeCount(product.id!!)
                }
            }
            val expectedIds = products.map { it.id!! }.sortedDescending()

            // act
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

            // assert
            assertThat(firstPage.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(secondPage.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(firstPage.body?.data?.content?.map { it.id }).containsExactlyElementsOf(expectedIds.take(10))
            assertThat(secondPage.body?.data?.content?.map { it.id }).containsExactlyElementsOf(expectedIds.drop(10))
        }

        @Test
        @DisplayName("brandId 필터가 없어도 LIKES_DESC 는 active 브랜드 전체에서 안정적으로 페이지네이션된다")
        fun getList_sortByLikesDesc_withoutBrandFilter_stableAcrossPages() {
            // arrange
            val brand2 = brandRepository.save(Brand.register(name = "아디다스"), ADMIN)
            val activeBrand2 = brandRepository.save(brand2.update("아디다스", "ACTIVE"), ADMIN)
            val products = (1..6).map { index ->
                createActiveProduct("나이키좋아요상품$index", 10000)
            } + (1..5).map { index ->
                createActiveProduct("아디다스좋아요상품$index", 10000, activeBrand2.id!!)
            }
            products.forEach { product ->
                repeat(3) {
                    productRepository.incrementLikeCount(product.id!!)
                }
            }
            val expectedIds = products.map { it.id!! }.sortedDescending()

            // act
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

            // assert
            assertThat(firstPage.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(secondPage.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(firstPage.body?.data?.content?.map { it.id }).containsExactlyElementsOf(expectedIds.take(10))
            assertThat(secondPage.body?.data?.content?.map { it.id }).containsExactlyElementsOf(expectedIds.drop(10))
        }

        @Test
        @DisplayName("brandId로 필터링할 수 있다")
        fun getList_filterByBrandId() {
            // arrange
            createActiveProduct("나이키 상품", 10000)

            val brand2 = brandRepository.save(Brand.register(name = "아디다스"), ADMIN)
            val activeBrand2 = brand2.update("아디다스", "ACTIVE")
            val savedBrand2 = brandRepository.save(activeBrand2, ADMIN)

            val otherProduct = Product.register(
                name = "아디다스 상품",
                regularPrice = Money(BigDecimal.valueOf(10000)),
                sellingPrice = Money(BigDecimal.valueOf(10000)),
                brandId = savedBrand2.id!!,
            )
            val savedOther = productRepository.save(otherProduct, ADMIN)
            productRepository.save(savedOther.activate(), ADMIN)

            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT?brandId=$brandId",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<PageResponse<UserProductV1Response.Summary>>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body?.data?.content).hasSize(1)
            assertThat(response.body?.data?.content?.first()?.name).isEqualTo("나이키 상품")
        }

        @Test
        @DisplayName("INACTIVE 브랜드의 ACTIVE 상품은 제외되고 totalElements도 일치한다")
        fun getList_excludeInactiveBrandProducts() {
            // arrange
            createActiveProduct("노출 상품", 10000)

            val inactiveBrand = brandRepository.save(Brand.register(name = "비활성 브랜드"), ADMIN)
            val hiddenProduct = Product.register(
                name = "숨김 상품",
                regularPrice = Money(BigDecimal.valueOf(10000)),
                sellingPrice = Money(BigDecimal.valueOf(10000)),
                brandId = inactiveBrand.id!!,
            )
            val savedHiddenProduct = productRepository.save(hiddenProduct, ADMIN)
            productRepository.save(savedHiddenProduct.activate(), ADMIN)

            // act
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<PageResponse<UserProductV1Response.Summary>>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body?.data?.totalElements).isEqualTo(1)
            assertThat(response.body?.data?.content).hasSize(1)
            assertThat(response.body?.data?.content?.first()?.name).isEqualTo("노출 상품")
        }
    }
}
