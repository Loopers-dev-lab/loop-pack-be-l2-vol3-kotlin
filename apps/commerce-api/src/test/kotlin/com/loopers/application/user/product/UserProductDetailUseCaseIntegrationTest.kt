package com.loopers.application.user.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStock
import com.loopers.domain.product.ProductStockRepository
import com.loopers.infrastructure.outbox.KafkaOutboxJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

@DisplayName("UserProductDetailUseCase integration")
@SpringBootTest
class UserProductDetailUseCaseIntegrationTest
@Autowired
constructor(
    private val userProductDetailUseCase: UserProductDetailUseCase,
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    private val productStockRepository: ProductStockRepository,
    private val kafkaOutboxJpaRepository: KafkaOutboxJpaRepository,
) {
    private var productId: Long = 0

    private var brandId: Long = 0

    @BeforeEach
    fun setUp() {
        val brand = brandRepository.save(Brand.register(name = "Detail Brand"), ADMIN)
        brandId = brandRepository.save(brand.update("Detail Brand", "ACTIVE"), ADMIN).id!!
    }

    @AfterEach
    fun tearDown() {
        if (productId != 0L) {
            productStockRepository.deleteByProductId(productId, ADMIN)
            productRepository.delete(productId, ADMIN)
        }
        if (brandId != 0L) {
            brandRepository.delete(brandId, ADMIN)
        }
        kafkaOutboxJpaRepository.deleteAll()
    }

    @Test
    @DisplayName("상품 상세 조회 시 ProductDetailViewedEvent 처리로 outbox row가 기록된다")
    fun getDetail_writesOutboxRow() {
        val product = createActiveProduct()
        productId = product.id!!

        val result = userProductDetailUseCase.getDetail(product.id!!)

        assertThat(result.id).isEqualTo(product.id)
        val rows = kafkaOutboxJpaRepository.findAll()
        assertThat(rows).hasSize(1)
        assertThat(rows[0].topic).isEqualTo("catalog-events")
        assertThat(rows[0].eventKey).isEqualTo(product.id.toString())
        assertThat(rows[0].publishedAt).isNull()
    }

    private fun createActiveProduct(): Product {
        val registered = Product.register(
            name = "Detail Product",
            regularPrice = Money(BigDecimal.valueOf(12_000)),
            sellingPrice = Money(BigDecimal.valueOf(10_000)),
            brandId = brandId,
            imageUrl = "https://example.com/image.png",
            thumbnailUrl = "https://example.com/thumb.png",
        )
        val saved = productRepository.save(registered, ADMIN)
        val active = productRepository.save(saved.activate(), ADMIN)
        productStockRepository.save(
            ProductStock.create(productId = active.id!!, initialQuantity = Quantity(10)),
            ADMIN,
        )
        return active
    }

    companion object {
        private const val ADMIN = "loopers.admin"
    }
}
