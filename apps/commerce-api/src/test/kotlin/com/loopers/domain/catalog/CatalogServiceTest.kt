package com.loopers.domain.catalog

import com.loopers.domain.catalog.brand.FakeBrandRepository
import com.loopers.domain.catalog.brand.vo.BrandName
import com.loopers.domain.catalog.product.FakeProductRepository
import com.loopers.domain.catalog.product.entity.Product
import com.loopers.domain.catalog.product.ProductSort
import com.loopers.domain.common.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

import java.math.BigDecimal

class CatalogServiceTest {

    private lateinit var brandRepository: FakeBrandRepository
    private lateinit var productRepository: FakeProductRepository
    private lateinit var catalogService: CatalogService

    @BeforeEach
    fun setUp() {
        brandRepository = FakeBrandRepository()
        productRepository = FakeProductRepository()
        catalogService = CatalogService(brandRepository, productRepository)
    }

    // === Brand 생성/수정 ===

    @Nested
    @DisplayName("브랜드 생성 시")
    inner class CreateBrand {

        @Test
        @DisplayName("유효한 이름으로 생성하면 Brand가 저장되고 반환된다")
        fun createBrand_withValidName_savesAndReturnsBrand() {
            // arrange
            val command = CatalogCommand.CreateBrand(name = BrandName("나이키"))

            // act
            val result = catalogService.createBrand(command)

            // assert
            assertThat(result.name.value).isEqualTo("나이키")
            assertThat(result.id).isNotEqualTo(0L)
        }
    }

    @Nested
    @DisplayName("브랜드 수정 시")
    inner class UpdateBrand {

        @Test
        @DisplayName("유효한 이름으로 수정하면 브랜드명이 변경된다")
        fun updateBrand_withValidName_updatesName() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))

            // act
            val result = catalogService.updateBrand(brand.id, CatalogCommand.UpdateBrand(name = BrandName("아디다스")))

            // assert
            assertThat(result.name.value).isEqualTo("아디다스")
        }

        @Test
        @DisplayName("존재하지 않는 브랜드를 수정하면 NOT_FOUND 예외가 발생한다")
        fun updateBrand_nonExistent_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                catalogService.updateBrand(999L, CatalogCommand.UpdateBrand(name = BrandName("아디다스")))
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("삭제된 브랜드를 수정하면 브랜드명이 변경된다")
        fun updateBrand_deletedBrand_updatesName() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            catalogService.deleteBrand(brand.id)

            // act
            val result = catalogService.updateBrand(brand.id, CatalogCommand.UpdateBrand(name = BrandName("아디다스")))

            // assert
            assertThat(result.name.value).isEqualTo("아디다스")
            assertThat(result.deletedAt).isNotNull()
        }
    }

    // === Brand 삭제 ===

    @Nested
    @DisplayName("브랜드 삭제 시")
    inner class DeleteBrand {

        @Test
        @DisplayName("브랜드를 삭제하면 soft delete되고 소속 상품도 cascade 삭제된다")
        fun deleteBrand_softDeletesWithCascade() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val product = catalogService.createProduct(
                CatalogCommand.CreateProduct(
                    brandId = brand.id,
                    name = "에어맥스",
                    price = Money(BigDecimal("129000")),
                    stock = 10,
                ),
            )

            // act
            catalogService.deleteBrand(brand.id)

            // assert
            assertThat(brand.deletedAt).isNotNull()
            val deletedProduct = productRepository.findById(product.id)
            assertThat(deletedProduct?.deletedAt).isNotNull()
        }

        @Test
        @DisplayName("존재하지 않는 브랜드를 삭제해도 예외가 발생하지 않는다 (멱등)")
        fun deleteBrand_nonExistent_isIdempotent() {
            // act & assert
            catalogService.deleteBrand(999L)
        }

        @Test
        @DisplayName("이미 삭제된 브랜드를 다시 삭제해도 예외가 발생하지 않는다 (멱등)")
        fun deleteBrand_alreadyDeleted_isIdempotent() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            catalogService.deleteBrand(brand.id)

            // act & assert
            catalogService.deleteBrand(brand.id)
        }
    }

    @Nested
    @DisplayName("브랜드 복구 시")
    inner class RestoreBrand {

        @Test
        @DisplayName("삭제된 브랜드를 복구하면 deletedAt이 null이 된다")
        fun restoreBrand_deletedBrand_restoresSuccessfully() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            catalogService.deleteBrand(brand.id)

            // act
            catalogService.restoreBrand(brand.id)

            // assert
            val restored = catalogService.getBrand(brand.id)
            assertThat(restored.deletedAt).isNull()
            assertThat(restored.name.value).isEqualTo("나이키")
        }

        @Test
        @DisplayName("삭제되지 않은 브랜드를 복구해도 정상 동작한다 (멱등)")
        fun restoreBrand_activeBrand_isIdempotent() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))

            // act & assert
            catalogService.restoreBrand(brand.id)
            assertThat(brand.deletedAt).isNull()
        }

        @Test
        @DisplayName("존재하지 않는 브랜드를 복구하면 NOT_FOUND 예외가 발생한다")
        fun restoreBrand_nonExistent_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                catalogService.restoreBrand(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    // === Brand 조회 ===

    @Nested
    @DisplayName("브랜드 조회 시")
    inner class GetBrand {

        @Test
        @DisplayName("getActiveBrand - 활성 브랜드를 조회하면 반환된다")
        fun getActiveBrand_activeBrand_returns() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))

            // act
            val result = catalogService.getActiveBrand(brand.id)

            // assert
            assertThat(result.name.value).isEqualTo("나이키")
        }

        @Test
        @DisplayName("getActiveBrand - 삭제된 브랜드를 조회하면 NOT_FOUND 예외가 발생한다")
        fun getActiveBrand_deletedBrand_throwsNotFound() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            catalogService.deleteBrand(brand.id)

            // act
            val exception = assertThrows<CoreException> {
                catalogService.getActiveBrand(brand.id)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("getActiveBrand - 존재하지 않는 브랜드를 조회하면 NOT_FOUND 예외가 발생한다")
        fun getActiveBrand_nonExistent_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                catalogService.getActiveBrand(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("getBrand - 삭제 포함하여 브랜드를 조회한다")
        fun getBrand_includesDeleted() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            catalogService.deleteBrand(brand.id)

            // act
            val result = catalogService.getBrand(brand.id)

            // assert
            assertThat(result.name.value).isEqualTo("나이키")
            assertThat(result.deletedAt).isNotNull()
        }

        @Test
        @DisplayName("getBrand - 존재하지 않는 브랜드를 조회하면 NOT_FOUND 예외가 발생한다")
        fun getBrand_nonExistent_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                catalogService.getBrand(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("getBrands - 브랜드 목록을 페이징하여 조회한다")
        fun getBrands_returnsPagedResults() {
            // arrange
            catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("아디다스")))
            catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("뉴발란스")))

            // act
            val result = catalogService.getBrands(0, 2)

            // assert
            assertThat(result.content).hasSize(2)
            assertThat(result.totalElements).isEqualTo(3)
        }
    }

    // === Product 생성 ===

    @Nested
    @DisplayName("상품 생성 시")
    inner class CreateProduct {

        @Test
        @DisplayName("유효한 정보로 생성하면 Product가 저장되고 반환된다")
        fun createProduct_withValidData_savesAndReturnsProduct() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val command = CatalogCommand.CreateProduct(
                brandId = brand.id,
                name = "에어맥스 90",
                price = Money(BigDecimal("129000")),
                stock = 100,
            )

            // act
            val result = catalogService.createProduct(command)

            // assert
            assertThat(result.name).isEqualTo("에어맥스 90")
            assertThat(result.price.value).isEqualByComparingTo(BigDecimal("129000"))
            assertThat(result.stock).isEqualTo(100)
            assertThat(result.status).isEqualTo(Product.ProductStatus.ON_SALE)
        }

        @Test
        @DisplayName("존재하지 않는 브랜드로 생성하면 NOT_FOUND 예외가 발생한다")
        fun createProduct_nonExistentBrand_throwsNotFound() {
            // arrange
            val command = CatalogCommand.CreateProduct(
                brandId = 999L,
                name = "에어맥스 90",
                price = Money(BigDecimal("129000")),
                stock = 100,
            )

            // act
            val exception = assertThrows<CoreException> {
                catalogService.createProduct(command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("삭제된 브랜드로 생성하면 NOT_FOUND 예외가 발생한다")
        fun createProduct_deletedBrand_throwsNotFound() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            catalogService.deleteBrand(brand.id)
            val command = CatalogCommand.CreateProduct(
                brandId = brand.id,
                name = "에어맥스 90",
                price = Money(BigDecimal("129000")),
                stock = 100,
            )

            // act
            val exception = assertThrows<CoreException> {
                catalogService.createProduct(command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    // === Product 수정/삭제 ===

    @Nested
    @DisplayName("상품 수정 시")
    inner class UpdateProduct {

        @Test
        @DisplayName("유효한 정보로 수정하면 상품이 변경된다")
        fun updateProduct_withValidData_updatesProduct() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val product = catalogService.createProduct(
                CatalogCommand.CreateProduct(
                    brandId = brand.id,
                    name = "에어맥스 90",
                    price = Money(BigDecimal("129000")),
                    stock = 100,
                ),
            )
            val command = CatalogCommand.UpdateProduct(
                name = "에어맥스 95",
                price = Money(BigDecimal("159000")),
                stock = 50,
                status = null,
            )

            // act
            val result = catalogService.updateProduct(product.id, command)

            // assert
            assertThat(result.name).isEqualTo("에어맥스 95")
            assertThat(result.price.value).isEqualByComparingTo(BigDecimal("159000"))
            assertThat(result.stock).isEqualTo(50)
        }

        @Test
        @DisplayName("삭제된 상품도 수정할 수 있다")
        fun updateProduct_deletedProduct_succeeds() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val product = catalogService.createProduct(
                CatalogCommand.CreateProduct(
                    brandId = brand.id,
                    name = "에어맥스 90",
                    price = Money(BigDecimal("129000")),
                    stock = 100,
                ),
            )
            catalogService.deleteProduct(product.id)

            // act
            val result = catalogService.updateProduct(
                product.id,
                CatalogCommand.UpdateProduct(name = "변경", price = null, stock = null, status = null),
            )

            // assert
            assertThat(result.name).isEqualTo("변경")
        }

        @Test
        @DisplayName("모든 필드가 null이면 BAD_REQUEST 예외가 발생한다")
        fun updateProduct_allFieldsNull_throwsBadRequest() {
            // arrange & act
            val exception = assertThrows<CoreException> {
                CatalogCommand.UpdateProduct(name = null, price = null, stock = null, status = null)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("수정할 항목이 최소 1개 이상 필요합니다.")
        }

        @Test
        @DisplayName("HIDDEN 상품도 어드민이 수정할 수 있다")
        fun updateProduct_hiddenProduct_success() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val product = catalogService.createProduct(
                CatalogCommand.CreateProduct(
                    brandId = brand.id,
                    name = "에어맥스 90",
                    price = Money(BigDecimal("129000")),
                    stock = 100,
                ),
            )
            catalogService.updateProduct(
                product.id,
                CatalogCommand.UpdateProduct(name = null, price = null, stock = null, status = Product.ProductStatus.HIDDEN),
            )

            // act
            val updated = catalogService.updateProduct(
                product.id,
                CatalogCommand.UpdateProduct(name = "변경", price = null, stock = null, status = null),
            )

            // assert
            assertThat(updated.name).isEqualTo("변경")
            assertThat(updated.status).isEqualTo(Product.ProductStatus.HIDDEN)
        }
    }

    @Nested
    @DisplayName("상품 삭제 시")
    inner class DeleteProduct {

        @Test
        @DisplayName("상품을 삭제하면 soft delete된다")
        fun deleteProduct_softDeletes() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val product = catalogService.createProduct(
                CatalogCommand.CreateProduct(
                    brandId = brand.id,
                    name = "에어맥스 90",
                    price = Money(BigDecimal("129000")),
                    stock = 100,
                ),
            )

            // act
            catalogService.deleteProduct(product.id)

            // assert
            val deleted = productRepository.findById(product.id)
            assertThat(deleted?.deletedAt).isNotNull()
        }

        @Test
        @DisplayName("존재하지 않는 상품을 삭제해도 예외가 발생하지 않는다 (멱등)")
        fun deleteProduct_nonExistent_isIdempotent() {
            // act & assert
            catalogService.deleteProduct(999L)
        }
    }

    @Nested
    @DisplayName("상품 복구 시")
    inner class RestoreProduct {

        @Test
        @DisplayName("삭제된 상품을 복구하면 deletedAt이 null이 된다")
        fun restoreProduct_deletedProduct_restoresSuccessfully() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val product = catalogService.createProduct(
                CatalogCommand.CreateProduct(
                    brandId = brand.id,
                    name = "에어맥스 90",
                    price = Money(BigDecimal("129000")),
                    stock = 100,
                ),
            )
            catalogService.deleteProduct(product.id)

            // act
            catalogService.restoreProduct(product.id)

            // assert
            val restored = catalogService.getProduct(product.id)
            assertThat(restored.deletedAt).isNull()
            assertThat(restored.name).isEqualTo("에어맥스 90")
        }

        @Test
        @DisplayName("삭제되지 않은 상품을 복구해도 정상 동작한다 (멱등)")
        fun restoreProduct_activeProduct_isIdempotent() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val product = catalogService.createProduct(
                CatalogCommand.CreateProduct(
                    brandId = brand.id,
                    name = "에어맥스 90",
                    price = Money(BigDecimal("129000")),
                    stock = 100,
                ),
            )

            // act & assert
            catalogService.restoreProduct(product.id)
            assertThat(product.deletedAt).isNull()
        }

        @Test
        @DisplayName("존재하지 않는 상품을 복구하면 NOT_FOUND 예외가 발생한다")
        fun restoreProduct_nonExistent_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                catalogService.restoreProduct(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    // === 대고객 조회 ===

    @Nested
    @DisplayName("대고객 상품 목록 조회 시")
    inner class GetProducts {

        @Test
        @DisplayName("삭제된 상품과 HIDDEN 상품은 제외된다")
        fun getProducts_excludesDeletedAndHidden() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand.id, name = "상품1", price = Money(BigDecimal("10000")), stock = 10),
            )
            val hidden = catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand.id, name = "상품2", price = Money(BigDecimal("20000")), stock = 10),
            )
            catalogService.updateProduct(
                hidden.id,
                CatalogCommand.UpdateProduct(name = null, price = null, stock = null, status = Product.ProductStatus.HIDDEN),
            )
            val deleted = catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand.id, name = "상품3", price = Money(BigDecimal("30000")), stock = 10),
            )
            catalogService.deleteProduct(deleted.id)

            // act
            val result = catalogService.getProducts(null, ProductSort.LATEST, 0, 10)

            // assert
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].name).isEqualTo("상품1")
        }

        @Test
        @DisplayName("brandId로 필터링한다")
        fun getProducts_filtersByBrandId() {
            // arrange
            val brand1 = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val brand2 = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("아디다스")))
            catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand1.id, name = "나이키 상품", price = Money(BigDecimal("10000")), stock = 10),
            )
            catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand2.id, name = "아디다스 상품", price = Money(BigDecimal("20000")), stock = 10),
            )

            // act
            val result = catalogService.getProducts(brand1.id, ProductSort.LATEST, 0, 10)

            // assert
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].name).isEqualTo("나이키 상품")
        }

        @Test
        @DisplayName("가격 오름차순으로 정렬한다")
        fun getProducts_sortsByPriceAsc() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand.id, name = "비싼상품", price = Money(BigDecimal("50000")), stock = 10),
            )
            catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand.id, name = "싼상품", price = Money(BigDecimal("10000")), stock = 10),
            )

            // act
            val result = catalogService.getProducts(null, ProductSort.PRICE_ASC, 0, 10)

            // assert
            assertThat(result.content[0].name).isEqualTo("싼상품")
            assertThat(result.content[1].name).isEqualTo("비싼상품")
        }

        @Test
        @DisplayName("좋아요 내림차순으로 정렬한다")
        fun getProducts_sortsByLikesDesc() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val product1 = catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand.id, name = "인기없는상품", price = Money(BigDecimal("10000")), stock = 10),
            )
            val product2 = catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand.id, name = "인기상품", price = Money(BigDecimal("20000")), stock = 10),
            )
            catalogService.increaseLikeCount(product2.id)
            catalogService.increaseLikeCount(product2.id)
            catalogService.increaseLikeCount(product1.id)

            // act
            val result = catalogService.getProducts(null, ProductSort.LIKES_DESC, 0, 10)

            // assert
            assertThat(result.content[0].name).isEqualTo("인기상품")
            assertThat(result.content[1].name).isEqualTo("인기없는상품")
        }
    }

    @Nested
    @DisplayName("상품 상세 조회 시")
    inner class GetProductDetail {

        @Test
        @DisplayName("ProductDetail(Product + Brand)을 반환한다")
        fun getProductDetail_returnsProductWithBrand() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val product = catalogService.createProduct(
                CatalogCommand.CreateProduct(
                    brandId = brand.id,
                    name = "에어맥스 90",
                    price = Money(BigDecimal("129000")),
                    stock = 100,
                ),
            )

            // act
            val result = catalogService.getProductDetail(product.id)

            // assert
            assertThat(result.product.name).isEqualTo("에어맥스 90")
            assertThat(result.brand.name.value).isEqualTo("나이키")
        }

        @Test
        @DisplayName("삭제된 상품을 조회하면 NOT_FOUND 예외가 발생한다")
        fun getProductDetail_deletedProduct_throwsNotFound() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val product = catalogService.createProduct(
                CatalogCommand.CreateProduct(
                    brandId = brand.id,
                    name = "에어맥스 90",
                    price = Money(BigDecimal("129000")),
                    stock = 100,
                ),
            )
            catalogService.deleteProduct(product.id)

            // act
            val exception = assertThrows<CoreException> {
                catalogService.getProductDetail(product.id)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    // === 내부용 조회 ===

    @Nested
    @DisplayName("내부용 상품 조회 시")
    inner class InternalProductQueries {

        @Test
        @DisplayName("getProduct - 삭제된 상품도 조회된다")
        fun getProduct_includesDeleted() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val product = catalogService.createProduct(
                CatalogCommand.CreateProduct(
                    brandId = brand.id,
                    name = "에어맥스 90",
                    price = Money(BigDecimal("129000")),
                    stock = 100,
                ),
            )
            catalogService.deleteProduct(product.id)

            // act
            val result = catalogService.getProduct(product.id)

            // assert
            assertThat(result.name).isEqualTo("에어맥스 90")
            assertThat(result.deletedAt).isNotNull()
        }

        @Test
        @DisplayName("getProduct - 존재하지 않는 상품을 조회하면 NOT_FOUND 예외가 발생한다")
        fun getProduct_nonExistent_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                catalogService.getProduct(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("getActiveProduct - 활성 상품을 조회하면 반환된다")
        fun getActiveProduct_activeProduct_returns() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val product = catalogService.createProduct(
                CatalogCommand.CreateProduct(
                    brandId = brand.id,
                    name = "에어맥스 90",
                    price = Money(BigDecimal("129000")),
                    stock = 100,
                ),
            )

            // act
            val result = catalogService.getActiveProduct(product.id)

            // assert
            assertThat(result.name).isEqualTo("에어맥스 90")
        }

        @Test
        @DisplayName("getActiveProduct - 삭제된 상품을 조회하면 NOT_FOUND 예외가 발생한다")
        fun getActiveProduct_deleted_throwsNotFound() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val product = catalogService.createProduct(
                CatalogCommand.CreateProduct(
                    brandId = brand.id,
                    name = "에어맥스 90",
                    price = Money(BigDecimal("129000")),
                    stock = 100,
                ),
            )
            catalogService.deleteProduct(product.id)

            // act
            val exception = assertThrows<CoreException> {
                catalogService.getActiveProduct(product.id)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("getActiveProduct - HIDDEN 상품을 조회하면 NOT_FOUND 예외가 발생한다")
        fun getActiveProduct_hidden_throwsNotFound() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val product = catalogService.createProduct(
                CatalogCommand.CreateProduct(
                    brandId = brand.id,
                    name = "에어맥스 90",
                    price = Money(BigDecimal("129000")),
                    stock = 100,
                ),
            )
            catalogService.updateProduct(
                product.id,
                CatalogCommand.UpdateProduct(name = null, price = null, stock = null, status = Product.ProductStatus.HIDDEN),
            )

            // act
            val exception = assertThrows<CoreException> {
                catalogService.getActiveProduct(product.id)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("getActiveProductsByIds - 삭제/HIDDEN 제외하고 일괄 조회한다")
        fun getActiveProductsByIds_excludesDeletedAndHidden() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val active = catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand.id, name = "활성상품", price = Money(BigDecimal("10000")), stock = 10),
            )
            val hidden = catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand.id, name = "숨김상품", price = Money(BigDecimal("20000")), stock = 10),
            )
            catalogService.updateProduct(
                hidden.id,
                CatalogCommand.UpdateProduct(name = null, price = null, stock = null, status = Product.ProductStatus.HIDDEN),
            )
            val deleted = catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand.id, name = "삭제상품", price = Money(BigDecimal("30000")), stock = 10),
            )
            catalogService.deleteProduct(deleted.id)

            // act
            val result = catalogService.getActiveProductsByIds(listOf(active.id, hidden.id, deleted.id))

            // assert
            assertThat(result).hasSize(1)
            assertThat(result[0].name).isEqualTo("활성상품")
        }
    }

    // === getProductsForOrder ===

    @Nested
    @DisplayName("주문용 상품 조회 시")
    inner class GetProductsForOrder {

        @Test
        @DisplayName("모든 상품이 ON_SALE이면 정상 반환된다")
        fun getProductsForOrder_allOnSale_returnsProducts() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val product1 = catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand.id, name = "상품1", price = Money(BigDecimal("10000")), stock = 10),
            )
            val product2 = catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand.id, name = "상품2", price = Money(BigDecimal("20000")), stock = 10),
            )

            // act
            val result = catalogService.getProductsForOrder(listOf(product1.id, product2.id))

            // assert
            assertThat(result).hasSize(2)
        }

        @Test
        @DisplayName("존재하지 않는 상품이 포함되면 BAD_REQUEST 예외가 발생한다")
        fun getProductsForOrder_missingProduct_throwsBadRequest() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val product = catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand.id, name = "상품1", price = Money(BigDecimal("10000")), stock = 10),
            )

            // act
            val exception = assertThrows<CoreException> {
                catalogService.getProductsForOrder(listOf(product.id, 999L))
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).contains("존재하지 않는 상품")
        }

        @Test
        @DisplayName("삭제된 상품이 포함되면 BAD_REQUEST 예외가 발생한다")
        fun getProductsForOrder_deletedProduct_throwsBadRequest() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val product = catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand.id, name = "상품1", price = Money(BigDecimal("10000")), stock = 10),
            )
            catalogService.deleteProduct(product.id)

            // act
            val exception = assertThrows<CoreException> {
                catalogService.getProductsForOrder(listOf(product.id))
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).contains("주문 가능한 상태가 아닌 상품")
        }

        @Test
        @DisplayName("판매 중이 아닌 상품이 포함되면 BAD_REQUEST 예외가 발생한다")
        fun getProductsForOrder_notOnSale_throwsBadRequest() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val product = catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand.id, name = "상품1", price = Money(BigDecimal("10000")), stock = 10),
            )
            catalogService.updateProduct(
                product.id,
                CatalogCommand.UpdateProduct(name = null, price = null, stock = null, status = Product.ProductStatus.HIDDEN),
            )

            // act
            val exception = assertThrows<CoreException> {
                catalogService.getProductsForOrder(listOf(product.id))
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).contains("주문 가능한 상태가 아닌 상품")
        }

        @Test
        @DisplayName("SOLD_OUT 상품이 포함되면 BAD_REQUEST 예외가 발생한다")
        fun getProductsForOrder_soldOut_throwsBadRequest() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            // stock=1로 생성 후 전량 차감 → 상태 자동 SOLD_OUT
            val product = catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand.id, name = "품절상품", price = Money(BigDecimal("10000")), stock = 1),
            )
            catalogService.decreaseStocks(mapOf(product.id to 1))

            // act
            val exception = assertThrows<CoreException> {
                catalogService.getProductsForOrder(listOf(product.id))
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).contains("주문 가능한 상태가 아닌 상품")
        }
    }

    // === 재고 차감 ===

    @Nested
    @DisplayName("재고 차감 시")
    inner class DecreaseStocks {

        @Test
        @DisplayName("정상적으로 재고가 차감된다")
        fun decreaseStocks_success() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val product1 = catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand.id, name = "상품1", price = Money(BigDecimal("10000")), stock = 10),
            )
            val product2 = catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand.id, name = "상품2", price = Money(BigDecimal("20000")), stock = 5),
            )

            // act
            catalogService.decreaseStocks(mapOf(product1.id to 3, product2.id to 2))

            // assert
            assertThat(productRepository.findById(product1.id)?.stock).isEqualTo(7)
            assertThat(productRepository.findById(product2.id)?.stock).isEqualTo(3)
        }

        @Test
        @DisplayName("재고 부족 시 CoreException이 발생한다")
        fun decreaseStocks_insufficientStock_throwsException() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val product = catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand.id, name = "상품1", price = Money(BigDecimal("10000")), stock = 5),
            )

            // act & assert
            assertThrows<CoreException> {
                catalogService.decreaseStocks(mapOf(product.id to 10))
            }
        }

        @Test
        @DisplayName("존재하지 않는 상품의 재고를 차감하면 NOT_FOUND 예외가 발생한다")
        fun decreaseStocks_nonExistentProduct_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                catalogService.decreaseStocks(mapOf(999L to 1))
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("재고를 전부 차감하면 상품 상태가 SOLD_OUT으로 전환된다")
        fun decreaseStocks_toZero_changesStatusToSoldOut() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val product = catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand.id, name = "에어맥스 90", price = Money(BigDecimal("129000")), stock = 5),
            )

            // act
            catalogService.decreaseStocks(mapOf(product.id to 5))

            // assert
            assertThat(productRepository.findById(product.id)?.status).isEqualTo(Product.ProductStatus.SOLD_OUT)
        }
    }

    // === likeCount ===

    @Nested
    @DisplayName("좋아요 수 변경 시")
    inner class LikeCount {

        @Test
        @DisplayName("increaseLikeCount - 좋아요 수가 1 증가한다")
        fun increaseLikeCount_incrementsByOne() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val product = catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand.id, name = "에어맥스 90", price = Money(BigDecimal("129000")), stock = 100),
            )

            // act
            catalogService.increaseLikeCount(product.id)

            // assert
            assertThat(productRepository.findById(product.id)?.likeCount).isEqualTo(1)
        }

        @Test
        @DisplayName("decreaseLikeCount - 좋아요 수가 1 감소한다")
        fun decreaseLikeCount_decrementsByOne() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val product = catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand.id, name = "에어맥스 90", price = Money(BigDecimal("129000")), stock = 100),
            )
            catalogService.increaseLikeCount(product.id)
            catalogService.increaseLikeCount(product.id)

            // act
            catalogService.decreaseLikeCount(product.id)

            // assert
            assertThat(productRepository.findById(product.id)?.likeCount).isEqualTo(1)
        }

        @Test
        @DisplayName("decreaseLikeCount - 좋아요 수가 0이면 0으로 유지된다")
        fun decreaseLikeCount_atZero_remainsZero() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val product = catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand.id, name = "에어맥스 90", price = Money(BigDecimal("129000")), stock = 100),
            )

            // act
            catalogService.decreaseLikeCount(product.id)

            // assert
            assertThat(productRepository.findById(product.id)?.likeCount).isEqualTo(0)
        }
    }

    // === 어드민 조회 ===

    @Nested
    @DisplayName("어드민 조회 시")
    inner class AdminQueries {

        @Test
        @DisplayName("getAdminProducts - 삭제된 상품도 포함하여 조회한다")
        fun getAdminProducts_includesDeleted() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand.id, name = "활성상품", price = Money(BigDecimal("10000")), stock = 10),
            )
            val deleted = catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand.id, name = "삭제상품", price = Money(BigDecimal("20000")), stock = 10),
            )
            catalogService.deleteProduct(deleted.id)

            // act
            val result = catalogService.getAdminProducts(0, 10)

            // assert
            assertThat(result.content).hasSize(2)
        }

        @Test
        @DisplayName("getAdminProduct - 삭제된 상품도 조회된다")
        fun getAdminProduct_includesDeleted() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = BrandName("나이키")))
            val product = catalogService.createProduct(
                CatalogCommand.CreateProduct(brandId = brand.id, name = "에어맥스 90", price = Money(BigDecimal("129000")), stock = 100),
            )
            catalogService.deleteProduct(product.id)

            // act
            val result = catalogService.getAdminProduct(product.id)

            // assert
            assertThat(result.name).isEqualTo("에어맥스 90")
            assertThat(result.deletedAt).isNotNull()
        }

        @Test
        @DisplayName("getAdminProduct - 존재하지 않는 상품을 조회하면 NOT_FOUND 예외가 발생한다")
        fun getAdminProduct_nonExistent_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                catalogService.getAdminProduct(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
