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

    private fun createActiveProduct(name: String, sellingPrice: Long): Product {
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
    }
}
