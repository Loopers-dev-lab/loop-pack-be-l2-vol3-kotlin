package com.loopers.domain.product

import com.loopers.domain.brand.Brand
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.math.BigDecimal

@DisplayName("ProductService")
class ProductServiceTest {
    private val productRepository: ProductRepository = mockk()
    private val productService = ProductService(productRepository)

    private fun createBrand(name: String = "Test Brand"): Brand {
        return Brand.create(name = name, description = "Test Description")
    }

    @DisplayName("상품을 생성할 때, ")
    @Nested
    inner class CreateProduct {
        @DisplayName("유효한 정보가 주어지면 상품이 생성되고 ID가 반환된다")
        @Test
        fun createsProduct_success() {
            // arrange
            val brand = createBrand()
            val name = "새로운 상품"
            val price = BigDecimal("15000.00")
            val stock = 50
            val status = ProductStatus.ACTIVE
            val productId = 1L

            val productSlot = slot<Product>()
            every { productRepository.save(capture(productSlot)) } answers {
                productSlot.captured.apply {
                    val idField = Product::class.java.superclass!!.getDeclaredField("id")
                    idField.isAccessible = true
                    idField.set(this, productId)
                }
            }

            // act
            val result = productService.createProduct(
                brand = brand,
                name = name,
                price = price,
                stock = stock,
                status = status,
            )

            // assert
            assertAll(
                { assertThat(result).isEqualTo(productId) },
                { assertThat(productSlot.captured.name).isEqualTo(name) },
                { assertThat(productSlot.captured.price).isEqualTo(price) },
                { assertThat(productSlot.captured.stock).isEqualTo(stock) },
                { assertThat(productSlot.captured.status).isEqualTo(status) },
                { assertThat(productSlot.captured.brand).isEqualTo(brand) },
            )
        }

        @DisplayName("가격이 0 미만이면 예외를 던진다")
        @Test
        fun createsProduct_throwsException_whenPriceIsNegative() {
            // arrange
            val brand = createBrand()

            // act & assert
            assertThatThrownBy {
                productService.createProduct(
                    brand = brand,
                    name = "상품",
                    price = BigDecimal("-100.00"),
                    stock = 100,
                    status = ProductStatus.ACTIVE,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("재고가 음수이면 예외를 던진다")
        @Test
        fun createsProduct_throwsException_whenStockIsNegative() {
            // arrange
            val brand = createBrand()

            // act & assert
            assertThatThrownBy {
                productService.createProduct(
                    brand = brand,
                    name = "상품",
                    price = BigDecimal("10000.00"),
                    stock = -10,
                    status = ProductStatus.ACTIVE,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("이름이 비어있으면 예외를 던진다")
        @Test
        fun createsProduct_throwsException_whenNameIsEmpty() {
            // arrange
            val brand = createBrand()

            // act & assert
            assertThatThrownBy {
                productService.createProduct(
                    brand = brand,
                    name = "",
                    price = BigDecimal("10000.00"),
                    stock = 100,
                    status = ProductStatus.ACTIVE,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("이름이 200자를 초과하면 예외를 던진다")
        @Test
        fun createsProduct_throwsException_whenNameExceeds200Chars() {
            // arrange
            val brand = createBrand()
            val longName = "a".repeat(201)

            // act & assert
            assertThatThrownBy {
                productService.createProduct(
                    brand = brand,
                    name = longName,
                    price = BigDecimal("10000.00"),
                    stock = 100,
                    status = ProductStatus.ACTIVE,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("상품을 수정할 때, ")
    @Nested
    inner class UpdateProduct {
        @DisplayName("유효한 정보가 주어지면 상품이 수정된다")
        @Test
        fun updatesProduct_success() {
            // arrange
            val brand = createBrand()
            val product = Product.create(
                brand = brand,
                name = "기존 상품",
                price = BigDecimal("10000.00"),
                stock = 100,
                status = ProductStatus.ACTIVE,
            )
            val productId = 1L
            val newName = "수정된 상품"
            val newPrice = BigDecimal("15000.00")
            val newStock = 50
            val newStatus = ProductStatus.INACTIVE

            every { productRepository.findById(productId) } returns product

            // act
            productService.updateProduct(
                id = productId,
                name = newName,
                price = newPrice,
                stock = newStock,
                status = newStatus,
            )

            // assert
            assertAll(
                { assertThat(product.name).isEqualTo(newName) },
                { assertThat(product.price).isEqualTo(newPrice) },
                { assertThat(product.stock).isEqualTo(newStock) },
                { assertThat(product.status).isEqualTo(newStatus) },
            )
        }

        @DisplayName("존재하지 않는 상품을 수정하려면 NOT_FOUND 예외를 던진다")
        @Test
        fun updatesProduct_throwsException_whenProductNotFound() {
            // arrange
            val productId = 999L

            every { productRepository.findById(productId) } throws CoreException(
                ErrorType.NOT_FOUND,
                "상품이 존재하지 않습니다.",
            )

            // act & assert
            assertThatThrownBy {
                productService.updateProduct(
                    id = productId,
                    name = "상품",
                    price = BigDecimal("10000.00"),
                    stock = 100,
                    status = ProductStatus.ACTIVE,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND)
        }

        @DisplayName("가격이 0 미만이면 예외를 던진다")
        @Test
        fun updatesProduct_throwsException_whenPriceIsNegative() {
            // arrange
            val brand = createBrand()
            val product = Product.create(
                brand = brand,
                name = "상품",
                price = BigDecimal("10000.00"),
                stock = 100,
            )
            val productId = 1L

            every { productRepository.findById(productId) } returns product

            // act & assert
            assertThatThrownBy {
                productService.updateProduct(
                    id = productId,
                    name = "상품",
                    price = BigDecimal("-100.00"),
                    stock = 100,
                    status = ProductStatus.ACTIVE,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("재고가 음수이면 예외를 던진다")
        @Test
        fun updatesProduct_throwsException_whenStockIsNegative() {
            // arrange
            val brand = createBrand()
            val product = Product.create(
                brand = brand,
                name = "상품",
                price = BigDecimal("10000.00"),
                stock = 100,
            )
            val productId = 1L

            every { productRepository.findById(productId) } returns product

            // act & assert
            assertThatThrownBy {
                productService.updateProduct(
                    id = productId,
                    name = "상품",
                    price = BigDecimal("10000.00"),
                    stock = -10,
                    status = ProductStatus.ACTIVE,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("상품을 삭제할 때, ")
    @Nested
    inner class DeleteProduct {
        @DisplayName("상품을 삭제하면 soft delete된다")
        @Test
        fun deletesProduct_success() {
            // arrange
            val brand = createBrand()
            val product = Product.create(
                brand = brand,
                name = "상품",
                price = BigDecimal("10000.00"),
                stock = 100,
                status = ProductStatus.ACTIVE,
            )
            val productId = 1L

            every { productRepository.findById(productId) } returns product

            // act
            productService.deleteProduct(productId)

            // assert
            assertThat(product.isDeleted()).isTrue()
        }

        @DisplayName("존재하지 않는 상품을 삭제하려면 NOT_FOUND 예외를 던진다")
        @Test
        fun deletesProduct_throwsException_whenProductNotFound() {
            // arrange
            val productId = 999L

            every { productRepository.findById(productId) } throws CoreException(
                ErrorType.NOT_FOUND,
                "상품이 존재하지 않습니다.",
            )

            // act & assert
            assertThatThrownBy {
                productService.deleteProduct(productId)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND)
        }

        @DisplayName("삭제된 상품을 다시 삭제하려면 NOT_FOUND 예외를 던진다")
        @Test
        fun deletesProduct_throwsException_whenProductAlreadyDeleted() {
            // arrange
            val brand = createBrand()
            val deletedProduct = Product.create(
                brand = brand,
                name = "삭제된 상품",
                price = BigDecimal("10000.00"),
                stock = 100,
            ).apply { delete() }
            val productId = 1L

            every { productRepository.findById(productId) } returns deletedProduct

            // act & assert
            assertThatThrownBy {
                productService.deleteProduct(productId)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("브랜드의 상품을 삭제할 때, ")
    @Nested
    inner class DeleteProductsByBrand {
        @DisplayName("브랜드의 여러 상품을 삭제하면 모두 soft delete된다")
        @Test
        fun deletesProductsByBrand_success() {
            // arrange
            val brand = createBrand()
            val brandId = 1L
            val product1 = Product.create(
                brand = brand,
                name = "상품1",
                price = BigDecimal("10000.00"),
                stock = 100,
                status = ProductStatus.ACTIVE,
            )
            val product2 = Product.create(
                brand = brand,
                name = "상품2",
                price = BigDecimal("20000.00"),
                stock = 50,
                status = ProductStatus.ACTIVE,
            )
            val product3 = Product.create(
                brand = brand,
                name = "상품3",
                price = BigDecimal("15000.00"),
                stock = 75,
                status = ProductStatus.ACTIVE,
            )
            val products = listOf(product1, product2, product3)

            every { productRepository.findByBrandId(brandId) } returns products

            // act
            productService.deleteProductsByBrand(brandId)

            // assert
            assertAll(
                { assertThat(product1.isDeleted()).isTrue() },
                { assertThat(product2.isDeleted()).isTrue() },
                { assertThat(product3.isDeleted()).isTrue() },
            )
        }

        @DisplayName("브랜드에 상품이 없으면 정상적으로 완료된다")
        @Test
        fun deletesProductsByBrand_success_whenNoProducts() {
            // arrange
            val brandId = 999L

            every { productRepository.findByBrandId(brandId) } returns emptyList()

            // act & assert (예외가 발생하지 않아야 함)
            productService.deleteProductsByBrand(brandId)
        }

        @DisplayName("브랜드의 모든 상품이 한 번에 삭제된다")
        @Test
        fun deletesProductsByBrand_deletesAll() {
            // arrange
            val brand = createBrand()
            val brandId = 1L
            val products = (1..5).map {
                Product.create(
                    brand = brand,
                    name = "상품$it",
                    price = BigDecimal("${10000 * it}.00"),
                    stock = 100 - it,
                    status = ProductStatus.ACTIVE,
                )
            }

            every { productRepository.findByBrandId(brandId) } returns products

            // act
            productService.deleteProductsByBrand(brandId)

            // assert
            assertThat(products).allMatch { it.isDeleted() }
        }
    }
}
