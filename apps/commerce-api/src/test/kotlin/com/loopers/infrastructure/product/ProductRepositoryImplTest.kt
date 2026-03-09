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

    @DisplayName("findWithPaging returns products sorted by likeCount descending")
    @Test
    fun findWithPaging_sortsByLikeCountDescending() {
        // Arrange
        val brand = createBrand()
        val product1 = createProduct(brand, "Product 1", BigDecimal("10000"), likeCount = 50)
        val product2 = createProduct(brand, "Product 2", BigDecimal("20000"), likeCount = 100)
        val product3 = createProduct(brand, "Product 3", BigDecimal("15000"), likeCount = 75)

        val pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("likeCount")))

        // Act
        val result = productRepository.findWithPaging(null, pageable)

        // Assert
        assertThat(result.content).hasSize(3)
        assertThat(result.content[0].id).isEqualTo(product2.id) // 100 likes
        assertThat(result.content[1].id).isEqualTo(product3.id) // 75 likes
        assertThat(result.content[2].id).isEqualTo(product1.id) // 50 likes
    }

    @DisplayName("findActiveProductsWithPaging returns active products sorted by likeCount")
    @Test
    fun findActiveProductsWithPaging_returnsActiveProductsSortedByLikeCount() {
        // Arrange
        val brand = createBrand()
        val activeProduct1 = createProduct(
            brand,
            "Active Product 1",
            BigDecimal("10000"),
            ProductStatus.ACTIVE,
            likeCount = 30,
        )
        val activeProduct2 = createProduct(
            brand,
            "Active Product 2",
            BigDecimal("20000"),
            ProductStatus.ACTIVE,
            likeCount = 60,
        )
        val inactiveProduct = createProduct(
            brand,
            "Inactive Product",
            BigDecimal("15000"),
            ProductStatus.INACTIVE,
            likeCount = 90,
        )

        val pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("likeCount")))

        // Act
        val result = productRepository.findActiveProductsWithPaging(null, pageable)

        // Assert
        assertThat(result.content).hasSize(2)
        assertThat(result.content[0].id).isEqualTo(activeProduct2.id) // 60 likes, ACTIVE
        assertThat(result.content[1].id).isEqualTo(activeProduct1.id) // 30 likes, ACTIVE
        assertThat(result.content.map { it.status }).allMatch { it == ProductStatus.ACTIVE }
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
        // Set likeCount through reflection since it's a protected property
        product::class.java.getDeclaredField("likeCount").apply {
            isAccessible = true
            setInt(product, likeCount)
        }
        val savedProduct = productRepository.save(product)
        return savedProduct
    }
}
