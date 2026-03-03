package com.loopers.infrastructure.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.support.page.PageRequest
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

@DisplayName("ProductRepository 통합 테스트")
@SpringBootTest
class ProductRepositoryIntegrationTest
@Autowired
constructor(
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    private val admin = "loopers.admin"

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createBrand(name: String = "나이키"): Brand {
        return brandRepository.save(Brand.register(name = name), admin)
    }

    private fun createProduct(
        name: String = "테스트 상품",
        regularPrice: Long = 10000,
        sellingPrice: Long = 8000,
        brandId: Long,
    ): Product {
        val product = Product.register(
            name = name,
            regularPrice = Money(BigDecimal.valueOf(regularPrice)),
            sellingPrice = Money(BigDecimal.valueOf(sellingPrice)),
            brandId = brandId,
        )
        return productRepository.save(product, admin)
    }

    @Nested
    @DisplayName("save 시")
    inner class WhenSave {
        @Test
        @DisplayName("저장 후 findById로 조회하면 동일한 상품을 반환한다")
        fun save_success() {
            // arrange
            val brand = createBrand()

            // act
            val saved = createProduct(brandId = brand.id!!)

            // assert
            val found = productRepository.findById(saved.id!!)
            assertAll(
                { assertThat(found).isNotNull },
                { assertThat(found!!.name).isEqualTo("테스트 상품") },
                { assertThat(found!!.regularPrice).isEqualTo(Money(BigDecimal.valueOf(10000))) },
                { assertThat(found!!.sellingPrice).isEqualTo(Money(BigDecimal.valueOf(8000))) },
                { assertThat(found!!.brandId).isEqualTo(brand.id) },
                { assertThat(found!!.status).isEqualTo(Product.Status.INACTIVE) },
            )
        }

        @Test
        @DisplayName("수정 후 저장하면 변경된 값으로 조회된다")
        fun save_update() {
            // arrange
            val brand = createBrand()
            val saved = createProduct(brandId = brand.id!!)
            val updated = saved.changeInfo(
                name = "변경 상품",
                regularPrice = Money(BigDecimal.valueOf(20000)),
                sellingPrice = Money(BigDecimal.valueOf(15000)),
                imageUrl = "https://img.test/new.jpg",
                thumbnailUrl = null,
            )

            // act
            productRepository.save(updated, admin)

            // assert
            val found = productRepository.findById(saved.id!!)
            assertAll(
                { assertThat(found!!.name).isEqualTo("변경 상품") },
                { assertThat(found!!.regularPrice).isEqualTo(Money(BigDecimal.valueOf(20000))) },
                { assertThat(found!!.sellingPrice).isEqualTo(Money(BigDecimal.valueOf(15000))) },
                { assertThat(found!!.imageUrl).isEqualTo("https://img.test/new.jpg") },
            )
        }
    }

    @Nested
    @DisplayName("delete 시")
    inner class WhenDelete {
        @Test
        @DisplayName("삭제된 상품은 findById로 조회되지 않는다")
        fun delete_softDelete() {
            // arrange
            val brand = createBrand()
            val saved = createProduct(brandId = brand.id!!)

            // act
            productRepository.delete(saved.id!!, admin)

            // assert
            val found = productRepository.findById(saved.id!!)
            assertThat(found).isNull()
        }
    }

    @Nested
    @DisplayName("findAllActive 시")
    inner class WhenFindAllActive {
        @Test
        @DisplayName("ACTIVE 상태인 상품만 조회된다")
        fun findAllActive_onlyActive() {
            // arrange
            val brand = createBrand()
            val product1 = createProduct(name = "상품1", brandId = brand.id!!)
            createProduct(name = "상품2", brandId = brand.id!!)
            val activated = product1.activate()
            productRepository.save(activated, admin)

            // act
            val result = productRepository.findAllActive(PageRequest(), null, null)

            // assert
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].name).isEqualTo("상품1")
        }

        @Test
        @DisplayName("brandId로 필터링할 수 있다")
        fun findAllActive_filterByBrandId() {
            // arrange
            val brand1 = createBrand("브랜드1")
            val brand2 = createBrand("브랜드2")
            val p1 = createProduct(name = "상품1", brandId = brand1.id!!)
            val p2 = createProduct(name = "상품2", brandId = brand2.id!!)
            productRepository.save(p1.activate(), admin)
            productRepository.save(p2.activate(), admin)

            // act
            val result = productRepository.findAllActive(PageRequest(), brand1.id, null)

            // assert
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].name).isEqualTo("상품1")
        }

        @Test
        @DisplayName("PRICE_ASC 정렬로 조회할 수 있다")
        fun findAllActive_sortByPriceAsc() {
            // arrange
            val brand = createBrand()
            val p1 = createProduct(name = "비싼상품", regularPrice = 20000, sellingPrice = 20000, brandId = brand.id!!)
            val p2 = createProduct(name = "싼상품", regularPrice = 5000, sellingPrice = 5000, brandId = brand.id!!)
            productRepository.save(p1.activate(), admin)
            productRepository.save(p2.activate(), admin)

            // act
            val result = productRepository.findAllActive(PageRequest(), null, Product.SortType.PRICE_ASC)

            // assert
            assertThat(result.content).hasSize(2)
            assertThat(result.content[0].name).isEqualTo("싼상품")
            assertThat(result.content[1].name).isEqualTo("비싼상품")
        }
    }

    @Nested
    @DisplayName("deleteAllByBrandId 시")
    inner class WhenDeleteAllByBrandId {
        @Test
        @DisplayName("해당 브랜드의 모든 상품이 soft delete된다")
        fun deleteAllByBrandId_success() {
            // arrange
            val brand = createBrand()
            createProduct(name = "상품1", brandId = brand.id!!)
            createProduct(name = "상품2", brandId = brand.id!!)

            // act
            productRepository.deleteAllByBrandId(brand.id!!, admin)

            // assert
            val products = productRepository.findAllByBrandId(brand.id!!)
            assertThat(products).isEmpty()
        }
    }
}
