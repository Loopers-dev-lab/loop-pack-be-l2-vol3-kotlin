package com.loopers.domain.product

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

@DisplayName("Product 도메인")
class ProductTest {

    private fun money(value: Long): Money = Money(BigDecimal.valueOf(value))

    @Nested
    @DisplayName("유효한 입력이면 등록에 성공한다")
    inner class WhenValidInput {
        @Test
        @DisplayName("정상 입력으로 등록하면 id=null, likeCount=0, status=INACTIVE로 생성된다")
        fun register_normalCase() {
            val product = Product.register(
                name = "테스트 상품",
                regularPrice = money(10000),
                sellingPrice = money(8000),
                brandId = 1L,
            )

            assertAll(
                { assertThat(product.id).isNull() },
                { assertThat(product.name).isEqualTo("테스트 상품") },
                { assertThat(product.regularPrice).isEqualTo(money(10000)) },
                { assertThat(product.sellingPrice).isEqualTo(money(8000)) },
                { assertThat(product.brandId).isEqualTo(1L) },
                { assertThat(product.likeCount).isEqualTo(0) },
                { assertThat(product.status).isEqualTo(Product.Status.INACTIVE) },
            )
        }

        @Test
        @DisplayName("1자 이름으로 등록한다 (경계값: 최소)")
        fun register_minLengthName() {
            val product = Product.register(
                name = "가",
                regularPrice = money(1000),
                sellingPrice = money(1000),
                brandId = 1L,
            )
            assertThat(product.name).isEqualTo("가")
        }

        @Test
        @DisplayName("100자 이름으로 등록한다 (경계값: 최대)")
        fun register_maxLengthName() {
            val name = "가".repeat(100)
            val product = Product.register(
                name = name,
                regularPrice = money(1000),
                sellingPrice = money(1000),
                brandId = 1L,
            )
            assertThat(product.name).isEqualTo(name)
        }

        @Test
        @DisplayName("정가와 판매가가 0원이면 등록에 성공한다")
        fun register_zeroPrice() {
            val product = Product.register(
                name = "무료 상품",
                regularPrice = money(0),
                sellingPrice = money(0),
                brandId = 1L,
            )
            assertAll(
                { assertThat(product.regularPrice).isEqualTo(money(0)) },
                { assertThat(product.sellingPrice).isEqualTo(money(0)) },
            )
        }

        @Test
        @DisplayName("판매가가 정가와 같으면 등록에 성공한다 (경계값)")
        fun register_samePrices() {
            val product = Product.register(
                name = "동일가 상품",
                regularPrice = money(5000),
                sellingPrice = money(5000),
                brandId = 1L,
            )
            assertAll(
                { assertThat(product.regularPrice).isEqualTo(money(5000)) },
                { assertThat(product.sellingPrice).isEqualTo(money(5000)) },
            )
        }
    }

    @Nested
    @DisplayName("상품명이 유효하지 않으면 등록에 실패한다")
    inner class WhenInvalidName {
        @Test
        @DisplayName("빈 문자열이면 예외를 던진다")
        fun register_emptyName() {
            val exception = assertThrows<CoreException> {
                Product.register(
                    name = "",
                    regularPrice = money(1000),
                    sellingPrice = money(1000),
                    brandId = 1L,
                )
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.PRODUCT_INVALID_NAME)
        }

        @Test
        @DisplayName("공백만 있으면 예외를 던진다")
        fun register_blankName() {
            val exception = assertThrows<CoreException> {
                Product.register(
                    name = "   ",
                    regularPrice = money(1000),
                    sellingPrice = money(1000),
                    brandId = 1L,
                )
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.PRODUCT_INVALID_NAME)
        }

        @Test
        @DisplayName("101자 이상이면 예외를 던진다")
        fun register_tooLongName() {
            val name = "가".repeat(101)
            val exception = assertThrows<CoreException> {
                Product.register(
                    name = name,
                    regularPrice = money(1000),
                    sellingPrice = money(1000),
                    brandId = 1L,
                )
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.PRODUCT_INVALID_NAME)
        }
    }

    @Nested
    @DisplayName("판매가가 정가보다 크면 등록에 실패한다")
    inner class WhenSellingPriceGreaterThanRegularPrice {
        @Test
        @DisplayName("판매가 > 정가이면 예외를 던진다")
        fun register_sellingPriceGreaterThanRegular() {
            val exception = assertThrows<CoreException> {
                Product.register(
                    name = "비싼 상품",
                    regularPrice = money(1000),
                    sellingPrice = money(2000),
                    brandId = 1L,
                )
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.PRODUCT_INVALID_PRICE)
        }
    }

    @Nested
    @DisplayName("changeInfo로 상품 정보를 변경한다")
    inner class WhenChangeInfo {
        @Test
        @DisplayName("이름, 가격, 이미지를 변경하고 brandId는 유지된다")
        fun changeInfo_success() {
            val product = Product.retrieve(
                id = 1L,
                name = "원래 상품",
                regularPrice = money(10000),
                sellingPrice = money(8000),
                brandId = 1L,
                imageUrl = null,
                thumbnailUrl = null,
                likeCount = 5,
                status = Product.Status.ACTIVE,
            )

            val changed = product.changeInfo(
                name = "변경 상품",
                regularPrice = money(15000),
                sellingPrice = money(12000),
                imageUrl = "https://img.test/new.jpg",
                thumbnailUrl = "https://img.test/new_thumb.jpg",
            )

            assertAll(
                { assertThat(changed.name).isEqualTo("변경 상품") },
                { assertThat(changed.regularPrice).isEqualTo(money(15000)) },
                { assertThat(changed.sellingPrice).isEqualTo(money(12000)) },
                { assertThat(changed.imageUrl).isEqualTo("https://img.test/new.jpg") },
                { assertThat(changed.thumbnailUrl).isEqualTo("https://img.test/new_thumb.jpg") },
                { assertThat(changed.brandId).isEqualTo(1L) },
                { assertThat(changed.likeCount).isEqualTo(5) },
            )
        }
    }

    @Nested
    @DisplayName("상태 변경")
    inner class WhenChangeStatus {
        @Test
        @DisplayName("activate하면 ACTIVE 상태가 된다")
        fun activate_success() {
            val product = Product.register(
                name = "상품",
                regularPrice = money(1000),
                sellingPrice = money(1000),
                brandId = 1L,
            )
            val activated = product.activate()
            assertThat(activated.status).isEqualTo(Product.Status.ACTIVE)
        }

        @Test
        @DisplayName("deactivate하면 INACTIVE 상태가 된다")
        fun deactivate_success() {
            val product = Product.retrieve(
                id = 1L,
                name = "상품",
                regularPrice = money(1000),
                sellingPrice = money(1000),
                brandId = 1L,
                imageUrl = null,
                thumbnailUrl = null,
                likeCount = 0,
                status = Product.Status.ACTIVE,
            )
            val deactivated = product.deactivate()
            assertThat(deactivated.status).isEqualTo(Product.Status.INACTIVE)
        }
    }
}
