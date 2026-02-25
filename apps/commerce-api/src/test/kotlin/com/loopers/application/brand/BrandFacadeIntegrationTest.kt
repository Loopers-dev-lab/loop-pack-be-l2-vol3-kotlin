package com.loopers.application.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
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

/**
 * BrandFacade 통합 테스트
 * - 실제 DB(TestContainers)와 연동하여 Facade → Service → Repository 레이어 통합 테스트
 * - Facade는 2+ 서비스 조합 시에만 사용 (cascade soft delete)
 */
@SpringBootTest
class BrandFacadeIntegrationTest @Autowired constructor(
    private val brandFacade: BrandFacade,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("브랜드를 삭제할 때,")
    @Nested
    inner class DeleteBrand {

        @DisplayName("브랜드 삭제 시, 해당 브랜드의 상품들도 cascade soft delete 된다.")
        @Test
        fun cascadeSoftDeletesProducts_whenBrandDeleted() {
            // arrange
            val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val product1 = productJpaRepository.save(
                Product(brandId = brand.id, name = "에어맥스 90", price = BigDecimal("129000"), stock = 100, description = null, imageUrl = null),
            )
            val product2 = productJpaRepository.save(
                Product(brandId = brand.id, name = "에어포스 1", price = BigDecimal("139000"), stock = 50, description = null, imageUrl = null),
            )

            // act
            brandFacade.deleteBrand(brand.id)

            // assert
            val deletedProduct1 = productJpaRepository.findById(product1.id).get()
            val deletedProduct2 = productJpaRepository.findById(product2.id).get()
            assertAll(
                { assertThat(deletedProduct1.isDeleted()).isTrue() },
                { assertThat(deletedProduct2.isDeleted()).isTrue() },
                { assertThat(productJpaRepository.findAllByBrandIdAndDeletedAtIsNull(brand.id)).isEmpty() },
            )
        }

        @DisplayName("브랜드 삭제 시, 다른 브랜드의 상품에는 영향을 주지 않는다.")
        @Test
        fun doesNotAffectOtherBrandProducts_whenBrandDeleted() {
            // arrange
            val brand1 = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val brand2 = brandJpaRepository.save(Brand(name = "아디다스", description = "독일 스포츠 브랜드"))
            productJpaRepository.save(
                Product(brandId = brand1.id, name = "에어맥스 90", price = BigDecimal("129000"), stock = 100, description = null, imageUrl = null),
            )
            val otherProduct = productJpaRepository.save(
                Product(brandId = brand2.id, name = "울트라부스트", price = BigDecimal("199000"), stock = 30, description = null, imageUrl = null),
            )

            // act
            brandFacade.deleteBrand(brand1.id)

            // assert
            val result = productJpaRepository.findByIdAndDeletedAtIsNull(otherProduct.id)
            assertThat(result).isNotNull()
            assertThat(result!!.name).isEqualTo("울트라부스트")
        }
    }
}
