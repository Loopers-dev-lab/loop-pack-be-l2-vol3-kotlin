package com.loopers.domain.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageRequest

class ProductServiceTest {

    private lateinit var productService: ProductService
    private lateinit var productRepository: FakeProductRepository

    @BeforeEach
    fun setUp() {
        productRepository = FakeProductRepository()
        productService = ProductService(productRepository)
    }

    private fun createCommand(
        brandId: Long = 1L,
        name: String = "에어맥스 90",
        description: String? = "나이키 에어맥스",
        price: Long = 139000,
        stockQuantity: Int = 100,
        displayYn: Boolean = true,
        imageUrl: String? = null,
    ) = CreateProductCommand(
        brandId = brandId,
        name = name,
        description = description,
        price = price,
        stockQuantity = stockQuantity,
        displayYn = displayYn,
        imageUrl = imageUrl,
    )

    @Nested
    inner class CreateProduct {

        @Test
        @DisplayName("올바른 정보로 상품을 생성하면 성공한다")
        fun success() {
            // arrange
            val command = createCommand()

            // act
            val product = productService.createProduct(command)

            // assert
            assertThat(product.name).isEqualTo("에어맥스 90")
            assertThat(product.price).isEqualTo(139000)
            assertThat(product.id).isGreaterThan(0)
        }

        @Test
        @DisplayName("상품명이 빈칸이면 BAD_REQUEST 예외가 발생한다")
        fun nameBlankThrowsBadRequest() {
            // arrange
            val command = createCommand(name = "   ")

            // act
            val result = assertThrows<CoreException> {
                productService.createProduct(command)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Nested
    inner class FindById {

        @Test
        @DisplayName("존재하는 상품을 조회하면 성공한다")
        fun success() {
            // arrange
            val saved = productService.createProduct(createCommand())

            // act
            val found = productService.findById(saved.id)

            // assert
            assertThat(found.name).isEqualTo("에어맥스 90")
        }

        @Test
        @DisplayName("존재하지 않는 상품을 조회하면 NOT_FOUND 예외가 발생한다")
        fun notFoundThrowsNotFound() {
            // act
            val result = assertThrows<CoreException> {
                productService.findById(999L)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Nested
    inner class UpdateProduct {

        @Test
        @DisplayName("존재하는 상품을 수정하면 성공한다")
        fun success() {
            // arrange
            val created = productService.createProduct(createCommand())
            val command = UpdateProductCommand(
                name = "에어포스 1",
                description = "클래식",
                price = 119000,
                stockQuantity = 50,
                status = ProductStatus.INACTIVE,
                displayYn = false,
                imageUrl = null,
            )

            // act
            val updated = productService.updateProduct(created.id, command)

            // assert
            assertThat(updated.name).isEqualTo("에어포스 1")
            assertThat(updated.status).isEqualTo(ProductStatus.INACTIVE)
        }

        @Test
        @DisplayName("존재하지 않는 상품을 수정하면 NOT_FOUND 예외가 발생한다")
        fun notFoundThrowsNotFound() {
            // arrange
            val command = UpdateProductCommand(
                name = "에어포스",
                description = null,
                price = 100000,
                stockQuantity = 10,
                status = ProductStatus.ACTIVE,
                displayYn = true,
                imageUrl = null,
            )

            // act
            val result = assertThrows<CoreException> {
                productService.updateProduct(999L, command)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Nested
    inner class DeleteProduct {

        @Test
        @DisplayName("존재하는 상품을 삭제하면 성공한다")
        fun success() {
            // arrange
            val created = productService.createProduct(createCommand())

            // act
            productService.deleteProduct(created.id)

            // assert
            val result = assertThrows<CoreException> {
                productService.findById(created.id)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("존재하지 않는 상품을 삭제하면 NOT_FOUND 예외가 발생한다")
        fun notFoundThrowsNotFound() {
            // act
            val result = assertThrows<CoreException> {
                productService.deleteProduct(999L)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Nested
    inner class DecreaseStock {

        @Test
        @DisplayName("충분한 재고가 있으면 재고가 차감된다")
        fun success() {
            // arrange
            val created = productService.createProduct(createCommand(stockQuantity = 10))

            // act
            productService.decreaseStock(created.id, 3)

            // assert
            val found = productService.findById(created.id)
            assertThat(found.stockQuantity).isEqualTo(7)
        }

        @Test
        @DisplayName("재고가 부족하면 BAD_REQUEST 예외가 발생한다")
        fun insufficientStockThrowsBadRequest() {
            // arrange
            val created = productService.createProduct(createCommand(stockQuantity = 2))

            // act
            val result = assertThrows<CoreException> {
                productService.decreaseStock(created.id, 3)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Nested
    inner class LikeCount {

        @Test
        @DisplayName("좋아요 수가 증가한다")
        fun increaseSuccess() {
            // arrange
            val created = productService.createProduct(createCommand())

            // act
            productService.increaseLikeCount(created.id)

            // assert
            val found = productService.findById(created.id)
            assertThat(found.likeCount).isEqualTo(1)
        }

        @Test
        @DisplayName("좋아요 수가 감소한다")
        fun decreaseSuccess() {
            // arrange
            val created = productService.createProduct(createCommand())
            productService.increaseLikeCount(created.id)
            productService.increaseLikeCount(created.id)

            // act
            productService.decreaseLikeCount(created.id)

            // assert
            val found = productService.findById(created.id)
            assertThat(found.likeCount).isEqualTo(1)
        }
    }

    @Nested
    inner class FindByBrandId {

        @Test
        @DisplayName("브랜드에 속한 상품 목록을 조회한다")
        fun success() {
            // arrange
            productService.createProduct(createCommand(brandId = 1L, name = "상품A"))
            productService.createProduct(createCommand(brandId = 1L, name = "상품B"))
            productService.createProduct(createCommand(brandId = 2L, name = "상품C"))

            // act
            val products = productService.findByBrandId(1L)

            // assert
            assertThat(products).hasSize(2)
        }
    }

    @Nested
    inner class FindAll {

        @Test
        @DisplayName("등록된 상품 목록을 조회하면 성공한다")
        fun success() {
            // arrange
            productService.createProduct(createCommand(name = "상품A"))
            productService.createProduct(createCommand(name = "상품B"))

            // act
            val products = productService.findAll()

            // assert
            assertThat(products).hasSize(2)
        }

        @Test
        @DisplayName("삭제된 상품은 목록에서 제외된다")
        fun excludeDeleted() {
            // arrange
            val created = productService.createProduct(createCommand(name = "상품A"))
            productService.createProduct(createCommand(name = "상품B"))
            productService.deleteProduct(created.id)

            // act
            val products = productService.findAll()

            // assert
            assertThat(products).hasSize(1)
            assertThat(products[0].name).isEqualTo("상품B")
        }
    }

    @Nested
    inner class FindAllForUser {

        @Test
        @DisplayName("대고객 페이징 조회 시 ACTIVE + displayYn=true 상품만 반환된다")
        fun success() {
            // arrange
            productService.createProduct(createCommand(name = "상품A"))
            productService.createProduct(createCommand(name = "상품B"))
            productService.createProduct(createCommand(name = "상품C", displayYn = false))

            // act
            val page = productService.findAllForUser(PageRequest.of(0, 20), null)

            // assert
            assertAll(
                { assertThat(page.content).hasSize(2) },
                { assertThat(page.totalElements).isEqualTo(2) },
            )
        }

        @Test
        @DisplayName("brandId로 필터링하여 조회할 수 있다")
        fun filterByBrandId() {
            // arrange
            productService.createProduct(createCommand(brandId = 1L, name = "상품A"))
            productService.createProduct(createCommand(brandId = 1L, name = "상품B"))
            productService.createProduct(createCommand(brandId = 2L, name = "상품C"))

            // act
            val page = productService.findAllForUser(PageRequest.of(0, 20), 1L)

            // assert
            assertAll(
                { assertThat(page.content).hasSize(2) },
                { assertThat(page.totalElements).isEqualTo(2) },
            )
        }

        @Test
        @DisplayName("INACTIVE 상품은 대고객 조회에서 제외된다")
        fun excludeInactive() {
            // arrange
            val created = productService.createProduct(createCommand(name = "상품A"))
            productService.createProduct(createCommand(name = "상품B"))
            created.update("상품A", null, 10000, 10, ProductStatus.INACTIVE, true, null)

            // act
            val page = productService.findAllForUser(PageRequest.of(0, 20), null)

            // assert
            assertThat(page.content).hasSize(1)
            assertThat(page.content[0].name).isEqualTo("상품B")
        }

        @Test
        @DisplayName("삭제된 상품은 대고객 조회에서 제외된다")
        fun excludeDeleted() {
            // arrange
            val created = productService.createProduct(createCommand(name = "상품A"))
            productService.createProduct(createCommand(name = "상품B"))
            productService.deleteProduct(created.id)

            // act
            val page = productService.findAllForUser(PageRequest.of(0, 20), null)

            // assert
            assertThat(page.content).hasSize(1)
        }

        @Test
        @DisplayName("페이징이 정상 동작한다")
        fun paging() {
            // arrange
            productService.createProduct(createCommand(name = "상품A"))
            productService.createProduct(createCommand(name = "상품B"))
            productService.createProduct(createCommand(name = "상품C"))

            // act
            val page = productService.findAllForUser(PageRequest.of(0, 2), null)

            // assert
            assertAll(
                { assertThat(page.content).hasSize(2) },
                { assertThat(page.totalElements).isEqualTo(3) },
                { assertThat(page.totalPages).isEqualTo(2) },
            )
        }
    }

    @Nested
    inner class FindAllForAdmin {

        @Test
        @DisplayName("어드민 페이징 조회 시 삭제되지 않은 모든 상품이 반환된다")
        fun success() {
            // arrange
            productService.createProduct(createCommand(name = "상품A"))
            productService.createProduct(createCommand(name = "상품B", displayYn = false))

            // act
            val page = productService.findAllForAdmin(PageRequest.of(0, 20), null)

            // assert
            assertThat(page.content).hasSize(2)
        }

        @Test
        @DisplayName("brandId로 필터링하여 조회할 수 있다")
        fun filterByBrandId() {
            // arrange
            productService.createProduct(createCommand(brandId = 1L, name = "상품A"))
            productService.createProduct(createCommand(brandId = 2L, name = "상품B"))

            // act
            val page = productService.findAllForAdmin(PageRequest.of(0, 20), 1L)

            // assert
            assertAll(
                { assertThat(page.content).hasSize(1) },
                { assertThat(page.content[0].name).isEqualTo("상품A") },
            )
        }

        @Test
        @DisplayName("삭제된 상품은 어드민 조회에서도 제외된다")
        fun excludeDeleted() {
            // arrange
            val created = productService.createProduct(createCommand(name = "상품A"))
            productService.createProduct(createCommand(name = "상품B"))
            productService.deleteProduct(created.id)

            // act
            val page = productService.findAllForAdmin(PageRequest.of(0, 20), null)

            // assert
            assertThat(page.content).hasSize(1)
        }

        @Test
        @DisplayName("INACTIVE 상품도 어드민 조회에 포함된다")
        fun includeInactive() {
            // arrange
            val created = productService.createProduct(createCommand(name = "상품A"))
            productService.createProduct(createCommand(name = "상품B"))
            created.update("상품A", null, 10000, 10, ProductStatus.INACTIVE, true, null)

            // act
            val page = productService.findAllForAdmin(PageRequest.of(0, 20), null)

            // assert
            assertThat(page.content).hasSize(2)
        }
    }
}
