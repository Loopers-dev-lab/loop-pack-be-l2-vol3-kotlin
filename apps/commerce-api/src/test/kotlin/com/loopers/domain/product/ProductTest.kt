package com.loopers.domain.product

import com.loopers.domain.brand.Brand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.math.BigDecimal

class ProductTest {
    private fun createBrand(name: String = "Test Brand", description: String = "Test Description"): Brand {
        return Brand.create(name = name, description = description)
    }

    @DisplayName("상품을 생성할 때, ")
    @Nested
    inner class Create {
        @DisplayName("유효한 정보가 주어지면 정상적으로 생성된다")
        @Test
        fun createsProduct_whenValidInfoIsProvided() {
            // arrange
            val brand = createBrand()
            val name = "테스트 상품"
            val price = BigDecimal("10000.00")
            val status = ProductStatus.ACTIVE

            // act
            val product = Product.create(
                brand = brand,
                name = name,
                price = price,
                status = status,
            )

            // assert
            assertAll(
                { assertThat(product.id).isNotNull() },
                { assertThat(product.brand).isEqualTo(brand) },
                { assertThat(product.name).isEqualTo(name) },
                { assertThat(product.price).isEqualTo(price) },
                { assertThat(product.status).isEqualTo(status) },
            )
        }

        @DisplayName("상태 파라미터가 없으면 ACTIVE 상태로 생성된다")
        @Test
        fun createsProductWithActiveStatus_whenStatusIsNotProvided() {
            // arrange
            val brand = createBrand()
            val name = "테스트 상품"
            val price = BigDecimal("10000.00")

            // act
            val product = Product.create(
                brand = brand,
                name = name,
                price = price,
            )

            // assert
            assertThat(product.status).isEqualTo(ProductStatus.ACTIVE)
        }
    }

    @DisplayName("상품 정보를 업데이트할 때, ")
    @Nested
    inner class UpdateInfo {
        @DisplayName("새로운 이름과 가격이 주어지면 상품 정보가 업데이트된다")
        @Test
        fun updatesProductInfo_whenNewNameAndPriceAreProvided() {
            // arrange
            val brand = createBrand()
            val product = Product.create(
                brand = brand,
                name = "기존 상품명",
                price = BigDecimal("10000.00"),
            )
            val newName = "새로운 상품명"
            val newPrice = BigDecimal("15000.00")

            // act
            product.updateInfo(newName, newPrice)

            // assert
            assertAll(
                { assertThat(product.name).isEqualTo(newName) },
                { assertThat(product.price).isEqualTo(newPrice) },
            )
        }
    }

    @DisplayName("상품 상태를 변경할 때, ")
    @Nested
    inner class ChangeStatus {
        @DisplayName("새로운 상태로 변경하면 상태가 업데이트된다")
        @Test
        fun changesProductStatus_whenNewStatusIsProvided() {
            // arrange
            val brand = createBrand()
            val product = Product.create(
                brand = brand,
                name = "상품",
                price = BigDecimal("10000.00"),
                status = ProductStatus.ACTIVE,
            )
            val newStatus = ProductStatus.INACTIVE

            // act
            product.changeStatus(newStatus)

            // assert
            assertThat(product.status).isEqualTo(newStatus)
        }
    }

    @DisplayName("상품 가용성을 확인할 때, ")
    @Nested
    inner class IsAvailable {
        @DisplayName("ACTIVE 상태이고 삭제되지 않으면 가용하다")
        @Test
        fun isAvailable_whenStatusIsActiveAndNotDeleted() {
            // arrange
            val brand = createBrand()
            val product = Product.create(
                brand = brand,
                name = "상품",
                price = BigDecimal("10000.00"),
                status = ProductStatus.ACTIVE,
            )

            // act & assert
            assertThat(product.isAvailable()).isTrue()
        }

        @DisplayName("INACTIVE 상태이면 가용하지 않다")
        @Test
        fun isNotAvailable_whenStatusIsInactive() {
            // arrange
            val brand = createBrand()
            val product = Product.create(
                brand = brand,
                name = "상품",
                price = BigDecimal("10000.00"),
                status = ProductStatus.INACTIVE,
            )

            // act & assert
            assertThat(product.isAvailable()).isFalse()
        }
    }

    @DisplayName("상품 삭제 상태를 확인할 때, ")
    @Nested
    inner class IsDeleted {
        @DisplayName("삭제되지 않으면 false를 반환한다")
        @Test
        fun isNotDeleted_whenDeletedAtIsNull() {
            // arrange
            val brand = createBrand()
            val product = Product.create(
                brand = brand,
                name = "상품",
                price = BigDecimal("10000.00"),
            )

            // act & assert
            assertThat(product.isDeleted()).isFalse()
        }
    }
}
