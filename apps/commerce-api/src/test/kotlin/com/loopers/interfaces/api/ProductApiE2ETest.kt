package com.loopers.interfaces.api

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.LikeCount
import com.loopers.domain.common.Money
import com.loopers.domain.common.StockQuantity
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.interfaces.api.product.ProductDto
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
class ProductApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val PRODUCT_LIST_ENDPOINT = "/api/v1/products"
        private const val PRODUCT_DETAIL_ENDPOINT = "/api/v1/products/{productId}"
        private val HTTP_ENTITY = HttpEntity<Void>(HttpHeaders())
        private val PAGE_RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<ProductDto.PageResponse>>() {}
        private val DETAIL_RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<ProductDto.DetailResponse>>() {}
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("GET /api/v1/products")
    @Nested
    inner class GetProducts {

        @DisplayName("상품이 존재하면, 200 OK와 상품 목록을 반환한다.")
        @Test
        fun returnsOkWithProducts_whenProductsExist() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            productRepository.save(
                Product(
                    name = "에어맥스",
                    description = "러닝화",
                    price = Money.of(159000L),
                    likes = LikeCount.of(10),
                    stockQuantity = StockQuantity.of(100),
                    brandId = brand.id,
                ),
            )
            productRepository.save(
                Product(
                    name = "에어포스",
                    description = "캐주얼화",
                    price = Money.of(139000L),
                    likes = LikeCount.of(20),
                    stockQuantity = StockQuantity.of(50),
                    brandId = brand.id,
                ),
            )

            // act
            val response = testRestTemplate.exchange(
                PRODUCT_LIST_ENDPOINT,
                HttpMethod.GET,
                HTTP_ENTITY,
                PAGE_RESPONSE_TYPE,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.content).hasSize(2) },
                { assertThat(response.body?.data?.totalElements).isEqualTo(2) },
            )
        }

        @DisplayName("brandId를 지정하면, 해당 브랜드의 상품만 반환한다.")
        @Test
        fun returnsFilteredProducts_whenBrandIdProvided() {
            // arrange
            val nike = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val adidas = brandRepository.save(Brand(name = "아디다스", description = "스포츠 브랜드"))
            productRepository.save(
                Product(
                    name = "에어맥스",
                    description = "러닝화",
                    price = Money.of(159000L),
                    likes = LikeCount.of(10),
                    stockQuantity = StockQuantity.of(100),
                    brandId = nike.id,
                ),
            )
            productRepository.save(
                Product(
                    name = "울트라부스트",
                    description = "러닝화",
                    price = Money.of(199000L),
                    likes = LikeCount.of(30),
                    stockQuantity = StockQuantity.of(80),
                    brandId = adidas.id,
                ),
            )
            productRepository.save(
                Product(
                    name = "에어포스",
                    description = "캐주얼화",
                    price = Money.of(139000L),
                    likes = LikeCount.of(20),
                    stockQuantity = StockQuantity.of(50),
                    brandId = nike.id,
                ),
            )

            // act
            val response = testRestTemplate.exchange(
                "$PRODUCT_LIST_ENDPOINT?brandId=${nike.id}",
                HttpMethod.GET,
                HTTP_ENTITY,
                PAGE_RESPONSE_TYPE,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.content).hasSize(2) },
                { assertThat(response.body?.data?.totalElements).isEqualTo(2) },
                {
                    assertThat(response.body?.data?.content?.all { it.brandId == nike.id })
                        .isTrue()
                },
            )
        }

        @DisplayName("sort=price_asc를 지정하면, 가격 오름차순으로 정렬된 상품 목록을 반환한다.")
        @Test
        fun returnsSortedByPriceAsc_whenSortIsPriceAsc() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            productRepository.save(
                Product(name = "에어맥스", description = "러닝화", price = Money.of(159000L), likes = LikeCount.of(10), stockQuantity = StockQuantity.of(100), brandId = brand.id),
            )
            productRepository.save(
                Product(name = "조던", description = "농구화", price = Money.of(239000L), likes = LikeCount.of(50), stockQuantity = StockQuantity.of(30), brandId = brand.id),
            )
            productRepository.save(
                Product(name = "에어포스", description = "캐주얼화", price = Money.of(139000L), likes = LikeCount.of(20), stockQuantity = StockQuantity.of(50), brandId = brand.id),
            )

            // act
            val response = testRestTemplate.exchange(
                "$PRODUCT_LIST_ENDPOINT?sort=price_asc",
                HttpMethod.GET,
                HTTP_ENTITY,
                PAGE_RESPONSE_TYPE,
            )

            // assert
            val prices = response.body?.data?.content?.map { it.price }
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(prices).containsExactly(139000, 159000, 239000) },
            )
        }

        @DisplayName("sort=likes_desc를 지정하면, 좋아요 내림차순으로 정렬된 상품 목록을 반환한다.")
        @Test
        fun returnsSortedByLikesDesc_whenSortIsLikesDesc() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            productRepository.save(
                Product(name = "에어맥스", description = "러닝화", price = Money.of(159000L), likes = LikeCount.of(10), stockQuantity = StockQuantity.of(100), brandId = brand.id),
            )
            productRepository.save(
                Product(name = "조던", description = "농구화", price = Money.of(239000L), likes = LikeCount.of(50), stockQuantity = StockQuantity.of(30), brandId = brand.id),
            )
            productRepository.save(
                Product(name = "에어포스", description = "캐주얼화", price = Money.of(139000L), likes = LikeCount.of(20), stockQuantity = StockQuantity.of(50), brandId = brand.id),
            )

            // act
            val response = testRestTemplate.exchange(
                "$PRODUCT_LIST_ENDPOINT?sort=likes_desc",
                HttpMethod.GET,
                HTTP_ENTITY,
                PAGE_RESPONSE_TYPE,
            )

            // assert
            val likes = response.body?.data?.content?.map { it.likes }
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(likes).containsExactly(50, 20, 10) },
            )
        }

        @DisplayName("page와 size를 지정하면, 해당 페이지의 상품만 반환하고 페이징 메타데이터를 포함한다.")
        @Test
        fun returnsPaginatedProducts_whenPageAndSizeProvided() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            repeat(5) { i ->
                productRepository.save(
                    Product(
                        name = "상품${i + 1}",
                        description = "설명",
                        price = Money.of((i + 1) * 10000L),
                        likes = LikeCount.of(0),
                        stockQuantity = StockQuantity.of(10),
                        brandId = brand.id,
                    ),
                )
            }

            // act
            val response = testRestTemplate.exchange(
                "$PRODUCT_LIST_ENDPOINT?page=0&size=2",
                HttpMethod.GET,
                HTTP_ENTITY,
                PAGE_RESPONSE_TYPE,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.content).hasSize(2) },
                { assertThat(response.body?.data?.page).isEqualTo(0) },
                { assertThat(response.body?.data?.size).isEqualTo(2) },
                { assertThat(response.body?.data?.totalElements).isEqualTo(5) },
                { assertThat(response.body?.data?.totalPages).isEqualTo(3) },
            )
        }

        @DisplayName("상품이 없으면, 200 OK와 빈 목록을 반환한다.")
        @Test
        fun returnsEmptyList_whenNoProductsExist() {
            // act
            val response = testRestTemplate.exchange(
                PRODUCT_LIST_ENDPOINT,
                HttpMethod.GET,
                HTTP_ENTITY,
                PAGE_RESPONSE_TYPE,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.content).isEmpty() },
                { assertThat(response.body?.data?.totalElements).isEqualTo(0) },
                { assertThat(response.body?.data?.totalPages).isEqualTo(0) },
            )
        }

        @DisplayName("삭제된 상품은 목록에서 제외된다.")
        @Test
        fun excludesDeletedProducts() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            productRepository.save(
                Product(name = "에어맥스", description = "러닝화", price = Money.of(159000L), likes = LikeCount.of(10), stockQuantity = StockQuantity.of(100), brandId = brand.id),
            )
            val deletedProduct = productRepository.save(
                Product(name = "단종상품", description = "단종", price = Money.of(99000L), likes = LikeCount.of(5), stockQuantity = StockQuantity.of(0), brandId = brand.id),
            )
            deletedProduct.delete()
            productRepository.save(deletedProduct)

            // act
            val response = testRestTemplate.exchange(
                PRODUCT_LIST_ENDPOINT,
                HttpMethod.GET,
                HTTP_ENTITY,
                PAGE_RESPONSE_TYPE,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.content).hasSize(1) },
                { assertThat(response.body?.data?.content?.first()?.name).isEqualTo("에어맥스") },
            )
        }

        @DisplayName("응답에 상품의 모든 필드가 포함된다.")
        @Test
        fun returnsAllProductFields() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val product = productRepository.save(
                Product(name = "에어맥스", description = "러닝화", price = Money.of(159000L), likes = LikeCount.of(10), stockQuantity = StockQuantity.of(100), brandId = brand.id),
            )

            // act
            val response = testRestTemplate.exchange(
                PRODUCT_LIST_ENDPOINT,
                HttpMethod.GET,
                HTTP_ENTITY,
                PAGE_RESPONSE_TYPE,
            )

            // assert
            val item = response.body?.data?.content?.first()
            assertAll(
                { assertThat(item?.id).isEqualTo(product.id) },
                { assertThat(item?.name).isEqualTo("에어맥스") },
                { assertThat(item?.description).isEqualTo("러닝화") },
                { assertThat(item?.price).isEqualTo(159000) },
                { assertThat(item?.likes).isEqualTo(10) },
                { assertThat(item?.stockQuantity).isEqualTo(100) },
                { assertThat(item?.brandId).isEqualTo(brand.id) },
            )
        }
    }

    @DisplayName("GET /api/v1/products/{productId}")
    @Nested
    inner class GetProduct {

        @DisplayName("상품이 존재하면, 200 OK와 상품 상세 정보를 반환한다.")
        @Test
        fun returnsOkWithProductDetail_whenProductExists() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val product = productRepository.save(
                Product(
                    name = "에어맥스",
                    description = "러닝화",
                    price = Money.of(159000L),
                    likes = LikeCount.of(10),
                    stockQuantity = StockQuantity.of(100),
                    brandId = brand.id,
                ),
            )

            // act
            val response = testRestTemplate.exchange(
                PRODUCT_DETAIL_ENDPOINT,
                HttpMethod.GET,
                HTTP_ENTITY,
                DETAIL_RESPONSE_TYPE,
                product.id,
            )

            // assert
            val data = response.body?.data
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(data?.id).isEqualTo(product.id) },
                { assertThat(data?.name).isEqualTo("에어맥스") },
                { assertThat(data?.price).isEqualTo(159000) },
                { assertThat(data?.description).isEqualTo("러닝화") },
                { assertThat(data?.brandId).isEqualTo(brand.id) },
                { assertThat(data?.brandName).isEqualTo("나이키") },
                { assertThat(data?.likeCount).isEqualTo(10) },
            )
        }

        @DisplayName("존재하지 않는 상품을 조회하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenProductDoesNotExist() {
            // act
            val response = testRestTemplate.exchange(
                PRODUCT_DETAIL_ENDPOINT,
                HttpMethod.GET,
                HTTP_ENTITY,
                DETAIL_RESPONSE_TYPE,
                999999L,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("삭제된 상품을 조회하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenProductIsDeleted() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val product = productRepository.save(
                Product(name = "단종상품", description = "단종", price = Money.of(99000L), likes = LikeCount.of(5), stockQuantity = StockQuantity.of(0), brandId = brand.id),
            )
            product.delete()
            productRepository.save(product)

            // act
            val response = testRestTemplate.exchange(
                PRODUCT_DETAIL_ENDPOINT,
                HttpMethod.GET,
                HTTP_ENTITY,
                DETAIL_RESPONSE_TYPE,
                product.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("description이 null인 상품도 정상 조회된다.")
        @Test
        fun returnsOk_whenDescriptionIsNull() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val product = productRepository.save(
                Product(name = "에어맥스", description = null, price = Money.of(159000L), likes = LikeCount.of(0), stockQuantity = StockQuantity.of(50), brandId = brand.id),
            )

            // act
            val response = testRestTemplate.exchange(
                PRODUCT_DETAIL_ENDPOINT,
                HttpMethod.GET,
                HTTP_ENTITY,
                DETAIL_RESPONSE_TYPE,
                product.id,
            )

            // assert
            val data = response.body?.data
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(data?.id).isEqualTo(product.id) },
                { assertThat(data?.description).isNull() },
            )
        }
    }
}
