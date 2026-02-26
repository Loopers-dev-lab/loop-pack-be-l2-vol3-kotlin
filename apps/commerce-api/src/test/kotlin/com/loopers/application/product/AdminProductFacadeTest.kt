package com.loopers.application.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandService
import com.loopers.domain.common.LikeCount
import com.loopers.domain.common.Money
import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import com.loopers.domain.common.SortOrder
import com.loopers.domain.common.StockQuantity
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import java.time.ZonedDateTime

@ExtendWith(MockitoExtension::class)
class AdminProductFacadeTest {

    @Mock
    private lateinit var productService: ProductService

    @Mock
    private lateinit var brandService: BrandService

    private lateinit var adminProductFacade: AdminProductFacade

    @BeforeEach
    fun setUp() {
        adminProductFacade = AdminProductFacade(productService, brandService)
    }

    @DisplayName("어드민 상품 목록 조회할 때,")
    @Nested
    inner class GetProducts {

        private val pageQuery = PageQuery(0, 20, SortOrder.UNSORTED)

        @DisplayName("상품 목록을 조회하면, 브랜드명이 포함된 AdminProductInfo를 반환한다.")
        @Test
        fun returnsAdminProductInfoWithBrandName() {
            // arrange
            val now = ZonedDateTime.now()
            val brand = Brand(name = "나이키", description = "스포츠 브랜드")
            ReflectionTestUtils.setField(brand, "id", 1L)

            val product = Product(
                name = "에어맥스",
                description = "러닝화",
                price = Money.of(159000L),
                likes = LikeCount.of(10),
                stockQuantity = StockQuantity.of(100),
                brandId = 1L,
            )
            ReflectionTestUtils.setField(product, "id", 1L)
            ReflectionTestUtils.setField(product, "createdAt", now)
            ReflectionTestUtils.setField(product, "updatedAt", now)

            val pageResult = PageResult(
                content = listOf(product),
                page = 0,
                size = 20,
                totalElements = 1,
                totalPages = 1,
            )

            whenever(productService.getProducts(null, pageQuery)).thenReturn(pageResult)
            whenever(brandService.getBrandsByIds(listOf(1L))).thenReturn(listOf(brand))

            // act
            val result = adminProductFacade.getProducts(null, pageQuery)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(1) },
                { assertThat(result.content.first().name).isEqualTo("에어맥스") },
                { assertThat(result.content.first().brandName).isEqualTo("나이키") },
                { assertThat(result.content.first().price).isEqualTo(159000L) },
                { assertThat(result.content.first().stockQuantity).isEqualTo(100) },
                { assertThat(result.content.first().likeCount).isEqualTo(10) },
                { assertThat(result.content.first().createdAt).isEqualTo(now) },
                { assertThat(result.content.first().updatedAt).isEqualTo(now) },
            )
        }

        @DisplayName("brandId를 지정하면, 해당 브랜드의 상품만 조회한다.")
        @Test
        fun returnsFilteredProducts_whenBrandIdProvided() {
            // arrange
            val now = ZonedDateTime.now()
            val brand = Brand(name = "나이키", description = "스포츠 브랜드")
            ReflectionTestUtils.setField(brand, "id", 1L)

            val product = Product(
                name = "에어맥스",
                description = "러닝화",
                price = Money.of(159000L),
                likes = LikeCount.of(10),
                stockQuantity = StockQuantity.of(100),
                brandId = 1L,
            )
            ReflectionTestUtils.setField(product, "id", 1L)
            ReflectionTestUtils.setField(product, "createdAt", now)
            ReflectionTestUtils.setField(product, "updatedAt", now)

            val pageResult = PageResult(
                content = listOf(product),
                page = 0,
                size = 20,
                totalElements = 1,
                totalPages = 1,
            )

            whenever(productService.getProducts(1L, pageQuery)).thenReturn(pageResult)
            whenever(brandService.getBrandsByIds(listOf(1L))).thenReturn(listOf(brand))

            // act
            val result = adminProductFacade.getProducts(1L, pageQuery)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(1) },
                { assertThat(result.content.first().brandId).isEqualTo(1L) },
                { assertThat(result.content.first().brandName).isEqualTo("나이키") },
                { assertThat(result.totalElements).isEqualTo(1) },
            )
        }

        @DisplayName("상품이 없으면, 빈 페이지를 반환한다.")
        @Test
        fun returnsEmptyPage_whenNoProducts() {
            // arrange
            val emptyResult = PageResult<Product>(
                content = emptyList(),
                page = 0,
                size = 20,
                totalElements = 0,
                totalPages = 0,
            )

            whenever(productService.getProducts(null, pageQuery)).thenReturn(emptyResult)

            // act
            val result = adminProductFacade.getProducts(null, pageQuery)

            // assert
            assertAll(
                { assertThat(result.content).isEmpty() },
                { assertThat(result.totalElements).isEqualTo(0) },
            )
        }
    }
}
