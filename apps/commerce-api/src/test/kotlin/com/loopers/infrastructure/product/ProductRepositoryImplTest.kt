package com.loopers.infrastructure.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStatus
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ProductRepositoryImpl")
class ProductRepositoryImplTest @Autowired constructor(
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun cleanup() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("findWithPaging supports multiple sort options")
    @Test
    fun findWithPaging_supportsMultipleSortOptions() {
        // Arrange
        val brand = createBrand()
        val product1 = createProduct(brand, "Product 1", BigDecimal("10000"), likeCount = 50)
        val product2 = createProduct(brand, "Product 2", BigDecimal("20000"), likeCount = 50)
        val product3 = createProduct(brand, "Product 3", BigDecimal("15000"), likeCount = 50)

        // Test with price DESC
        val pageablePrice = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("price")))

        // Act - Sort by price descending
        val resultByPrice = productRepository.findWithPaging(null, pageablePrice)

        // Assert - Price sorting
        assertThat(resultByPrice.content).hasSize(3)
        assertThat(resultByPrice.content[0].price).isEqualByComparingTo(BigDecimal("20000")) // product2
        assertThat(resultByPrice.content[1].price).isEqualByComparingTo(BigDecimal("15000")) // product3
        assertThat(resultByPrice.content[2].price).isEqualByComparingTo(BigDecimal("10000")) // product1

        // Test with createdAt DESC
        val pageableCreatedAt = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt")))

        // Act - Sort by createdAt descending
        val resultByCreatedAt = productRepository.findWithPaging(null, pageableCreatedAt)

        // Assert - CreatedAt sorting (latest first)
        assertThat(resultByCreatedAt.content).hasSize(3)
        // Since createdAt is set during entity creation, the last created should be first
        assertThat(resultByCreatedAt.content[2].id).isEqualTo(product1.id)
        assertThat(resultByCreatedAt.content[1].id).isEqualTo(product2.id)
        assertThat(resultByCreatedAt.content[0].id).isEqualTo(product3.id)
    }

    private fun createBrand(): Brand {
        return brandRepository.save(
            Brand.create(
                name = "Test Brand ${System.nanoTime()}",
                description = "Test Brand Description",
            ),
        )
    }

    private fun createProduct(
        brand: Brand,
        name: String,
        price: BigDecimal,
        status: ProductStatus = ProductStatus.ACTIVE,
        likeCount: Int = 0,
    ): Product {
        val product = Product.create(
            brand = brand,
            name = name,
            price = price,
            status = status,
        )
        val savedProduct = productRepository.save(product)
        return savedProduct
    }
}
