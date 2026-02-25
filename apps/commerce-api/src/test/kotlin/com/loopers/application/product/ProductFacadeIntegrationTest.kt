package com.loopers.application.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.CreateProductCommand
import com.loopers.domain.product.Product
import com.loopers.domain.product.UpdateProductCommand
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal

/**
 * ProductFacade 통합 테스트
 * - 실제 DB(TestContainers)와 연동하여 Facade → Service → Repository 레이어 통합 테스트
 * - @Transactional 경계가 Facade에 있으므로 Facade를 통해 테스트
 */
@SpringBootTest
class ProductFacadeIntegrationTest @Autowired constructor(
    private val productFacade: ProductFacade,
    private val productJpaRepository: ProductJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createBrand(name: String = "나이키"): Brand {
        return brandJpaRepository.save(Brand(name = name, description = "스포츠 브랜드"))
    }

    private fun createProduct(
        brandId: Long,
        name: String = "에어맥스 90",
        price: BigDecimal = BigDecimal("129000"),
        stock: Int = 100,
        description: String? = "나이키 에어맥스 90",
        imageUrl: String? = "https://example.com/airmax90.jpg",
    ): Product {
        return productJpaRepository.save(
            Product(
                brandId = brandId,
                name = name,
                price = price,
                stock = stock,
                description = description,
                imageUrl = imageUrl,
            ),
        )
    }

    @DisplayName("상품을 조회할 때,")
    @Nested
    inner class GetProduct {

        @DisplayName("존재하는 상품 ID로 조회하면, 상품 정보가 반환된다.")
        @Test
        fun returnsProduct_whenProductExists() {
            // arrange
            val brand = createBrand()
            val saved = createProduct(brandId = brand.id)

            // act
            val result = productFacade.getProduct(saved.id)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(saved.id) },
                { assertThat(result.name).isEqualTo("에어맥스 90") },
                { assertThat(result.brandId).isEqualTo(brand.id) },
                { assertThat(result.price).isEqualByComparingTo(BigDecimal("129000")) },
                { assertThat(result.stock).isEqualTo(100) },
            )
        }

        @DisplayName("존재하지 않는 상품 ID로 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenProductNotExists() {
            // act & assert
            val exception = assertThrows<CoreException> {
                productFacade.getProduct(999L)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("soft delete된 상품은 조회되지 않는다.")
        @Test
        fun throwsNotFound_whenProductIsSoftDeleted() {
            // arrange
            val brand = createBrand()
            val saved = createProduct(brandId = brand.id)
            saved.delete()
            productJpaRepository.save(saved)

            // act & assert
            val exception = assertThrows<CoreException> {
                productFacade.getProduct(saved.id)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("상품 목록을 조회할 때,")
    @Nested
    inner class GetAllProducts {

        @DisplayName("전체 상품이 페이징 조회된다.")
        @Test
        fun returnsAllProducts_whenNoBrandIdFilter() {
            // arrange
            val brand = createBrand()
            createProduct(brandId = brand.id, name = "에어맥스 90")
            createProduct(brandId = brand.id, name = "에어포스 1")

            // act
            val result = productFacade.getAllProducts(brandId = null, pageable = PageRequest.of(0, 20))

            // assert
            assertAll(
                { assertThat(result.content).hasSize(2) },
                { assertThat(result.totalElements).isEqualTo(2L) },
            )
        }

        @DisplayName("brandId로 필터링하면, 해당 브랜드 상품만 반환된다.")
        @Test
        fun returnsFilteredProducts_whenBrandIdProvided() {
            // arrange
            val brand1 = createBrand(name = "나이키")
            val brand2 = createBrand(name = "아디다스")
            createProduct(brandId = brand1.id, name = "에어맥스 90")
            createProduct(brandId = brand2.id, name = "울트라부스트")

            // act
            val result = productFacade.getAllProducts(brandId = brand1.id, pageable = PageRequest.of(0, 20))

            // assert
            assertAll(
                { assertThat(result.content).hasSize(1) },
                { assertThat(result.content[0].brandId).isEqualTo(brand1.id) },
            )
        }

        @DisplayName("soft delete된 상품은 목록에 포함되지 않는다.")
        @Test
        fun excludesSoftDeletedProducts() {
            // arrange
            val brand = createBrand()
            createProduct(brandId = brand.id, name = "에어맥스 90")
            val deleted = createProduct(brandId = brand.id, name = "에어포스 1")
            deleted.delete()
            productJpaRepository.save(deleted)

            // act
            val result = productFacade.getAllProducts(brandId = null, pageable = PageRequest.of(0, 20))

            // assert
            assertAll(
                { assertThat(result.content).hasSize(1) },
                { assertThat(result.content[0].name).isEqualTo("에어맥스 90") },
            )
        }
    }

    @DisplayName("상품을 등록할 때,")
    @Nested
    inner class CreateProduct {

        @DisplayName("정상적인 정보가 주어지면, 상품이 DB에 저장된다.")
        @Test
        fun savesProductToDatabase_whenValidInfoProvided() {
            // arrange
            val brand = createBrand()
            val command = CreateProductCommand(
                brandId = brand.id,
                name = "에어맥스 90",
                price = BigDecimal("129000"),
                stock = 100,
                description = "나이키 에어맥스 90",
                imageUrl = "https://example.com/airmax90.jpg",
            )

            // act
            val result = productFacade.createProduct(command)

            // assert
            val saved = productJpaRepository.findByIdAndDeletedAtIsNull(result.id)!!
            assertAll(
                { assertThat(saved.name).isEqualTo("에어맥스 90") },
                { assertThat(saved.brandId).isEqualTo(brand.id) },
                { assertThat(saved.price).isEqualByComparingTo(BigDecimal("129000")) },
                { assertThat(saved.stock).isEqualTo(100) },
            )
        }

        @DisplayName("존재하지 않는 브랜드 ID로 등록하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenBrandNotExists() {
            // arrange
            val command = CreateProductCommand(
                brandId = 999L,
                name = "에어맥스 90",
                price = BigDecimal("129000"),
                stock = 100,
                description = null,
                imageUrl = null,
            )

            // act & assert
            val exception = assertThrows<CoreException> {
                productFacade.createProduct(command)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("상품을 수정할 때,")
    @Nested
    inner class UpdateProduct {

        @DisplayName("정상적인 정보가 주어지면, 상품이 DB에서 수정된다.")
        @Test
        fun updatesProductInDatabase_whenValidInfoProvided() {
            // arrange
            val brand = createBrand()
            val saved = createProduct(brandId = brand.id)
            val command = UpdateProductCommand(
                name = "에어포스 1",
                price = BigDecimal("139000"),
                stock = 50,
                description = "나이키 에어포스 1",
                imageUrl = "https://example.com/airforce1.jpg",
            )

            // act
            productFacade.updateProduct(saved.id, command)

            // assert
            val updated = productJpaRepository.findByIdAndDeletedAtIsNull(saved.id)!!
            assertAll(
                { assertThat(updated.name).isEqualTo("에어포스 1") },
                { assertThat(updated.price).isEqualByComparingTo(BigDecimal("139000")) },
                { assertThat(updated.stock).isEqualTo(50) },
                { assertThat(updated.brandId).isEqualTo(brand.id) },
            )
        }

        @DisplayName("존재하지 않는 상품을 수정하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenProductNotExists() {
            // arrange
            val command = UpdateProductCommand(
                name = "에어포스 1",
                price = BigDecimal("139000"),
                stock = 50,
                description = null,
                imageUrl = null,
            )

            // act & assert
            val exception = assertThrows<CoreException> {
                productFacade.updateProduct(999L, command)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("상품을 삭제할 때,")
    @Nested
    inner class DeleteProduct {

        @DisplayName("존재하는 상품을 삭제하면, soft delete 되어 조회되지 않는다.")
        @Test
        fun softDeletesProduct_whenProductExists() {
            // arrange
            val brand = createBrand()
            val saved = createProduct(brandId = brand.id)

            // act
            productFacade.deleteProduct(saved.id)

            // assert
            val deleted = productJpaRepository.findById(saved.id).get()
            assertAll(
                { assertThat(deleted.isDeleted()).isTrue() },
                { assertThat(productJpaRepository.findByIdAndDeletedAtIsNull(saved.id)).isNull() },
            )
        }

        @DisplayName("존재하지 않는 상품을 삭제하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenProductNotExists() {
            // act & assert
            val exception = assertThrows<CoreException> {
                productFacade.deleteProduct(999L)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
