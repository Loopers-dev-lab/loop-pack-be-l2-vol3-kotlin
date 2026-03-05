package com.loopers.domain.order

import com.loopers.domain.common.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

@DisplayName("OrderSnapshot VO")
class OrderSnapshotTest {

    private fun createSnapshot(
        productId: Long = 1L,
        productName: String = "테스트 상품",
        brandId: Long = 1L,
        brandName: String = "테스트 브랜드",
        regularPrice: Money = Money(BigDecimal("10000")),
        sellingPrice: Money = Money(BigDecimal("8000")),
        thumbnailUrl: String? = "https://example.com/image.jpg",
    ): OrderSnapshot = OrderSnapshot(
        productId = productId,
        productName = productName,
        brandId = brandId,
        brandName = brandName,
        regularPrice = regularPrice,
        sellingPrice = sellingPrice,
        thumbnailUrl = thumbnailUrl,
    )

    @Nested
    @DisplayName("생성")
    inner class Create {

        @Test
        @DisplayName("유효한 값으로 생성 성공")
        fun create_success() {
            // act
            val snapshot = createSnapshot()

            // assert
            assertAll(
                { assertThat(snapshot.productId).isEqualTo(1L) },
                { assertThat(snapshot.productName).isEqualTo("테스트 상품") },
                { assertThat(snapshot.brandId).isEqualTo(1L) },
                { assertThat(snapshot.brandName).isEqualTo("테스트 브랜드") },
                { assertThat(snapshot.regularPrice).isEqualTo(Money(BigDecimal("10000"))) },
                { assertThat(snapshot.sellingPrice).isEqualTo(Money(BigDecimal("8000"))) },
                { assertThat(snapshot.thumbnailUrl).isEqualTo("https://example.com/image.jpg") },
            )
        }

        @Test
        @DisplayName("thumbnailUrl이 null이어도 생성 성공")
        fun create_nullThumbnailUrl() {
            // act
            val snapshot = createSnapshot(thumbnailUrl = null)

            // assert
            assertThat(snapshot.thumbnailUrl).isNull()
        }
    }

    @Nested
    @DisplayName("상품명이 blank이면 생성 실패한다")
    inner class WhenProductNameBlank {

        @Test
        @DisplayName("빈 상품명 → 실패")
        fun create_emptyProductName() {
            val exception = assertThrows<CoreException> {
                createSnapshot(productName = "")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.PRODUCT_INVALID_NAME)
        }

        @Test
        @DisplayName("공백 상품명 → 실패")
        fun create_blankProductName() {
            val exception = assertThrows<CoreException> {
                createSnapshot(productName = "   ")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.PRODUCT_INVALID_NAME)
        }
    }

    @Nested
    @DisplayName("브랜드명이 blank이면 생성 실패한다")
    inner class WhenBrandNameBlank {

        @Test
        @DisplayName("빈 브랜드명 → 실패")
        fun create_emptyBrandName() {
            val exception = assertThrows<CoreException> {
                createSnapshot(brandName = "")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BRAND_INVALID_NAME)
        }
    }

    @Nested
    @DisplayName("판매가가 정가를 초과하면 생성 실패한다")
    inner class WhenSellingPriceExceedsRegularPrice {

        @Test
        @DisplayName("판매가 > 정가 → 실패")
        fun create_sellingPriceGreaterThanRegularPrice() {
            val exception = assertThrows<CoreException> {
                createSnapshot(
                    regularPrice = Money(BigDecimal("10000")),
                    sellingPrice = Money(BigDecimal("15000")),
                )
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.PRODUCT_INVALID_PRICE)
        }

        @Test
        @DisplayName("판매가 == 정가 → 성공 (경계값)")
        fun create_sellingPriceEqualsRegularPrice() {
            // act
            val snapshot = createSnapshot(
                regularPrice = Money(BigDecimal("10000")),
                sellingPrice = Money(BigDecimal("10000")),
            )

            // assert
            assertThat(snapshot.sellingPrice).isEqualTo(snapshot.regularPrice)
        }
    }
}
