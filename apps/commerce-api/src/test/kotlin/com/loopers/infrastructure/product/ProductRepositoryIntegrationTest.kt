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
    private val productJpaRepository: ProductJpaRepository,
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

    private fun createActiveBrand(name: String = "나이키"): Brand {
        val brand = createBrand(name)
        return brandRepository.save(brand.update(name, "ACTIVE"), admin)
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
            val brand = createBrand()

            val saved = createProduct(brandId = brand.id!!)

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
            val brand = createBrand()
            val saved = createProduct(brandId = brand.id!!)
            val updated = saved.changeInfo(
                name = "변경 상품",
                regularPrice = Money(BigDecimal.valueOf(20000)),
                sellingPrice = Money(BigDecimal.valueOf(15000)),
                imageUrl = "https://img.test/new.jpg",
                thumbnailUrl = null,
            )

            productRepository.save(updated, admin)

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
            val brand = createBrand()
            val saved = createProduct(brandId = brand.id!!)

            productRepository.delete(saved.id!!, admin)

            val found = productRepository.findById(saved.id!!)
            assertThat(found).isNull()
        }
    }

    @Nested
    @DisplayName("incrementLikeCount 시")
    inner class WhenIncrementLikeCount {
        @Test
        @DisplayName("ACTIVE 상품에 incrementLikeCount 호출 시 likeCount가 증가한다")
        fun incrementLikeCount_activeProduct_increasesLikeCount() {
            val brand = createBrand()
            val saved = productRepository.save(createProduct(brandId = brand.id!!).activate(), admin)

            productRepository.incrementLikeCount(saved.id!!)

            val entity = productJpaRepository.findById(saved.id!!).get()
            assertThat(entity.likeCount).isEqualTo(1)
        }

        @Test
        @DisplayName("INACTIVE 상품에 incrementLikeCount 호출 시 likeCount가 변하지 않는다")
        fun incrementLikeCount_inactiveProduct_noChange() {
            val brand = createBrand()
            val saved = createProduct(brandId = brand.id!!) // INACTIVE by default

            productRepository.incrementLikeCount(saved.id!!)

            val entity = productJpaRepository.findById(saved.id!!).get()
            assertThat(entity.likeCount).isEqualTo(0)
        }

        @Test
        @DisplayName("soft-delete된 상품에 incrementLikeCount 호출 시 likeCount가 변하지 않는다")
        fun incrementLikeCount_deletedProduct_noChange() {
            val brand = createBrand()
            val saved = productRepository.save(createProduct(brandId = brand.id!!).activate(), admin)
            productRepository.delete(saved.id!!, admin)

            productRepository.incrementLikeCount(saved.id!!)

            val entity = productJpaRepository.findById(saved.id!!).get()
            assertThat(entity.likeCount).isEqualTo(0)
        }

        @Test
        @DisplayName("incrementLikeCount 후 updatedAt이 변하지 않는다")
        fun incrementLikeCount_auditUpdatedAt_unchanged() {
            val brand = createBrand()
            val saved = productRepository.save(createProduct(brandId = brand.id!!).activate(), admin)
            val before = productJpaRepository.findById(saved.id!!).get()
            val updatedAtBefore = before.updatedAt

            productRepository.incrementLikeCount(saved.id!!)

            val after = productJpaRepository.findById(saved.id!!).get()
            assertThat(after.updatedAt).isEqualTo(updatedAtBefore)
        }

        @Test
        @DisplayName("incrementLikeCount 후 updatedBy가 변하지 않는다")
        fun incrementLikeCount_auditUpdatedBy_unchanged() {
            val brand = createBrand()
            val saved = productRepository.save(createProduct(brandId = brand.id!!).activate(), admin)
            val before = productJpaRepository.findById(saved.id!!).get()
            val updatedByBefore = before.updatedBy

            productRepository.incrementLikeCount(saved.id!!)

            val after = productJpaRepository.findById(saved.id!!).get()
            assertThat(after.updatedBy).isEqualTo(updatedByBefore)
        }
    }

    @Nested
    @DisplayName("decrementLikeCount 시")
    inner class WhenDecrementLikeCount {
        @Test
        @DisplayName("likeCount > 0인 상품에 decrementLikeCount 호출 시 likeCount가 감소한다")
        fun decrementLikeCount_positive_decreases() {
            val brand = createBrand()
            val saved = productRepository.save(createProduct(brandId = brand.id!!).activate(), admin)
            productRepository.incrementLikeCount(saved.id!!)

            productRepository.decrementLikeCount(saved.id!!)

            val entity = productJpaRepository.findById(saved.id!!).get()
            assertThat(entity.likeCount).isEqualTo(0)
        }

        @Test
        @DisplayName("likeCount = 0인 상품에 decrementLikeCount 호출 시 likeCount가 0으로 유지된다")
        fun decrementLikeCount_zero_noChange() {
            val brand = createBrand()
            val saved = productRepository.save(createProduct(brandId = brand.id!!).activate(), admin)

            productRepository.decrementLikeCount(saved.id!!)

            val entity = productJpaRepository.findById(saved.id!!).get()
            assertThat(entity.likeCount).isEqualTo(0)
        }

        @Test
        @DisplayName("decrementLikeCount 후 updatedAt이 변하지 않는다")
        fun decrementLikeCount_auditUpdatedAt_unchanged() {
            val brand = createBrand()
            val saved = productRepository.save(createProduct(brandId = brand.id!!).activate(), admin)
            productRepository.incrementLikeCount(saved.id!!)
            val before = productJpaRepository.findById(saved.id!!).get()
            val updatedAtBefore = before.updatedAt

            productRepository.decrementLikeCount(saved.id!!)

            val after = productJpaRepository.findById(saved.id!!).get()
            assertThat(after.updatedAt).isEqualTo(updatedAtBefore)
        }

        @Test
        @DisplayName("decrementLikeCount 후 updatedBy가 변하지 않는다")
        fun decrementLikeCount_auditUpdatedBy_unchanged() {
            val brand = createBrand()
            val saved = productRepository.save(createProduct(brandId = brand.id!!).activate(), admin)
            productRepository.incrementLikeCount(saved.id!!)
            val before = productJpaRepository.findById(saved.id!!).get()
            val updatedByBefore = before.updatedBy

            productRepository.decrementLikeCount(saved.id!!)

            val after = productJpaRepository.findById(saved.id!!).get()
            assertThat(after.updatedBy).isEqualTo(updatedByBefore)
        }
    }

    @Nested
    @DisplayName("findAllActive 시")
    inner class WhenFindAllActive {
        @Test
        @DisplayName("ACTIVE 상태인 상품만 조회된다")
        fun findAllActive_onlyActive() {
            val brand = createActiveBrand()
            val product1 = createProduct(name = "상품1", brandId = brand.id!!)
            createProduct(name = "상품2", brandId = brand.id!!)
            val activated = product1.activate()
            productRepository.save(activated, admin)

            val result = productRepository.findAllActive(PageRequest(), null, null)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].name).isEqualTo("상품1")
        }

        @Test
        @DisplayName("brandId로 필터링할 수 있다")
        fun findAllActive_filterByBrandId() {
            val brand1 = createActiveBrand("브랜드1")
            val brand2 = createActiveBrand("브랜드2")
            val p1 = createProduct(name = "상품1", brandId = brand1.id!!)
            val p2 = createProduct(name = "상품2", brandId = brand2.id!!)
            productRepository.save(p1.activate(), admin)
            productRepository.save(p2.activate(), admin)

            val result = productRepository.findAllActive(PageRequest(), brand1.id, null)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].name).isEqualTo("상품1")
        }

        @Test
        @DisplayName("PRICE_ASC 정렬로 조회할 수 있다")
        fun findAllActive_sortByPriceAsc() {
            val brand = createActiveBrand()
            val p1 = createProduct(name = "비싼상품", regularPrice = 20000, sellingPrice = 20000, brandId = brand.id!!)
            val p2 = createProduct(name = "싼상품", regularPrice = 5000, sellingPrice = 5000, brandId = brand.id!!)
            productRepository.save(p1.activate(), admin)
            productRepository.save(p2.activate(), admin)

            val result = productRepository.findAllActive(PageRequest(), null, Product.SortType.PRICE_ASC)

            assertThat(result.content).hasSize(2)
            assertThat(result.content[0].name).isEqualTo("싼상품")
            assertThat(result.content[1].name).isEqualTo("비싼상품")
        }

        @Test
        @DisplayName("INACTIVE 브랜드의 ACTIVE 상품은 조회되지 않고 totalElements도 일치한다")
        fun findAllActive_excludesInactiveBrandProducts() {
            val activeBrand = createActiveBrand("활성 브랜드")
            val inactiveBrand = createBrand("비활성 브랜드")
            val activeProduct = createProduct(name = "노출 상품", brandId = activeBrand.id!!)
            val hiddenProduct = createProduct(name = "숨김 상품", brandId = inactiveBrand.id!!)
            productRepository.save(activeProduct.activate(), admin)
            productRepository.save(hiddenProduct.activate(), admin)

            val result = productRepository.findAllActive(PageRequest(), null, null)

            assertThat(result.content).hasSize(1)
            assertThat(result.totalElements).isEqualTo(1)
            assertThat(result.content[0].name).isEqualTo("노출 상품")
        }

        @Test
        @DisplayName("LIKES_DESC 정렬로 조회할 수 있다")
        fun findAllActive_sortByLikesDesc() {
            val brand = createActiveBrand()
            val lowLikes = productRepository.save(createProduct(name = "낮은좋아요", brandId = brand.id!!).activate(), admin)
            val highLikes = productRepository.save(createProduct(name = "높은좋아요", brandId = brand.id!!).activate(), admin)

            productRepository.incrementLikeCount(lowLikes.id!!)
            repeat(3) {
                productRepository.incrementLikeCount(highLikes.id!!)
            }

            val result = productRepository.findAllActive(PageRequest(), null, Product.SortType.LIKES_DESC)

            assertThat(result.content).hasSize(2)
            assertThat(result.content[0].name).isEqualTo("높은좋아요")
            assertThat(result.content[1].name).isEqualTo("낮은좋아요")
        }
    }

    @Nested
    @DisplayName("deleteAllByBrandId 시")
    inner class WhenDeleteAllByBrandId {
        @Test
        @DisplayName("해당 브랜드의 모든 상품이 soft delete된다")
        fun deleteAllByBrandId_success() {
            val brand = createBrand()
            createProduct(name = "상품1", brandId = brand.id!!)
            createProduct(name = "상품2", brandId = brand.id!!)

            productRepository.deleteAllByBrandId(brand.id!!, admin)

            val products = productRepository.findAllByBrandId(brand.id!!)
            assertThat(products).isEmpty()
        }
    }
}
