package com.loopers.domain.productlike

import com.loopers.domain.brand.Brand
import com.loopers.domain.outbox.OutboxPublisher
import com.loopers.domain.product.Product
import com.loopers.domain.user.User
import com.loopers.domain.user.vo.BirthDate
import com.loopers.domain.user.vo.Email
import com.loopers.domain.user.vo.LoginId
import com.loopers.domain.user.vo.Name
import com.loopers.domain.user.vo.Password
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.infrastructure.productlike.ProductLikeCountJpaRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.crypto.password.PasswordEncoder
import java.math.BigDecimal

@DisplayName("ProductLikeCountBatchService 통합 테스트")
@SpringBootTest
class ProductLikeCountBatchServiceTest @Autowired constructor(
    private val productLikeCountBatchService: ProductLikeCountBatchService,
    private val productLikeService: ProductLikeService,
    private val productLikeCountRepository: ProductLikeCountRepository,
    private val productLikeCountJpaRepository: ProductLikeCountJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val passwordEncoder: PasswordEncoder,
) {
    @MockBean
    private lateinit var outboxPublisher: OutboxPublisher

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createTestUser(idx: Int): User {
        return User.create(
            loginId = LoginId.of("user$idx"),
            password = Password.ofEncrypted(passwordEncoder.encode("password123")),
            name = Name.of("사용자$idx"),
            birthDate = BirthDate.of("20000101"),
            email = Email.of("user$idx@test.com"),
        ).also { userJpaRepository.save(it) }
    }

    private fun createTestProduct(brandName: String, productName: String): Product {
        val brand = Brand.create(name = brandName, description = "Test")
        brandJpaRepository.save(brand)
        val product = Product.create(
            brand = brand,
            name = productName,
            price = BigDecimal("10000.00"),
        )
        return productJpaRepository.save(product)
    }

    @DisplayName("배치가 좋아요 없는 상품을 제대로 처리한다 (스킵)")
    @Test
    fun reconcileBatch_skipsWhenNoLikes() {
        // arrange
        val product = createTestProduct("Brand1", "Product1")

        // act
        productLikeCountBatchService.reconcileProductLikeCount()

        // assert - ProductLikeCount 테이블에 아무것도 저장되지 않아야 함
        val counts = productLikeCountJpaRepository.findAll()
        assertThat(counts).isEmpty()
    }

    @DisplayName("배치가 정확하게 좋아요를 집계하고 저장한다")
    @Test
    fun reconcileBatch_aggregatesAndSavesLikesCorrectly() {
        // arrange
        val product1 = createTestProduct("Brand1", "Product1")
        val product2 = createTestProduct("Brand2", "Product2")
        val users = (1..5).map { createTestUser(it) }

        // product1에 3명이 좋아요
        productLikeService.addProductLike(users[0], product1)
        productLikeService.addProductLike(users[1], product1)
        productLikeService.addProductLike(users[2], product1)

        // product2에 2명이 좋아요
        productLikeService.addProductLike(users[3], product2)
        productLikeService.addProductLike(users[4], product2)

        // act
        productLikeCountBatchService.reconcileProductLikeCount()

        // assert
        val product1Count = productLikeCountRepository.findByProductId(product1.id)
        assertThat(product1Count).isNotNull
        assertThat(product1Count!!.likeCount).isEqualTo(3L)

        val product2Count = productLikeCountRepository.findByProductId(product2.id)
        assertThat(product2Count).isNotNull
        assertThat(product2Count!!.likeCount).isEqualTo(2L)
    }

    @DisplayName("배치가 여러 상품의 좋아요를 동시에 집계한다")
    @Test
    fun reconcileBatch_aggregatesMultipleProductsSimultaneously() {
        // arrange
        val products = (1..3).map { createTestProduct("Brand$it", "Product$it") }
        val users = (1..10).map { createTestUser(it) }

        // 각 상품에 다른 수의 좋아요 추가
        (0..2).forEach { userIndex ->
            productLikeService.addProductLike(users[userIndex], products[0])
        }

        (0..4).forEach { userIndex ->
            productLikeService.addProductLike(users[userIndex], products[1])
        }

        (0..8).forEach { userIndex ->
            productLikeService.addProductLike(users[userIndex], products[2])
        }

        // act
        productLikeCountBatchService.reconcileProductLikeCount()

        // assert
        assertThat(productLikeCountRepository.findByProductId(products[0].id)!!.likeCount).isEqualTo(3L)
        assertThat(productLikeCountRepository.findByProductId(products[1].id)!!.likeCount).isEqualTo(5L)
        assertThat(productLikeCountRepository.findByProductId(products[2].id)!!.likeCount).isEqualTo(9L)
    }

    @DisplayName("배치가 기존 ProductLikeCount를 올바르게 업데이트한다")
    @Test
    fun reconcileBatch_updatesExistingProductLikeCountCorrectly() {
        // arrange
        val product = createTestProduct("Brand1", "Product1")
        val users = (1..5).map { createTestUser(it) }

        // 첫 번째 배치: 3명이 좋아요
        productLikeService.addProductLike(users[0], product)
        productLikeService.addProductLike(users[1], product)
        productLikeService.addProductLike(users[2], product)

        productLikeCountBatchService.reconcileProductLikeCount()

        var productLikeCount = productLikeCountRepository.findByProductId(product.id)
        assertThat(productLikeCount!!.likeCount).isEqualTo(3L)

        // 두 번째 배치 전: 2명이 더 좋아요 추가
        productLikeService.addProductLike(users[3], product)
        productLikeService.addProductLike(users[4], product)

        // act - 두 번째 배치 실행
        productLikeCountBatchService.reconcileProductLikeCount()

        // assert - 새로운 개수로 업데이트되어야 함
        productLikeCount = productLikeCountRepository.findByProductId(product.id)
        assertThat(productLikeCount!!.likeCount).isEqualTo(5L)
    }

    @DisplayName("배치가 좋아요 삭제 후 정확하게 재집계한다")
    @Test
    fun reconcileBatch_recalculatesAccuratelyAfterLikeRemoval() {
        // arrange
        val product = createTestProduct("Brand1", "Product1")
        val users = (1..5).map { createTestUser(it) }

        // 5명이 좋아요
        users.forEach { user ->
            productLikeService.addProductLike(user, product)
        }

        productLikeCountBatchService.reconcileProductLikeCount()

        var productLikeCount = productLikeCountRepository.findByProductId(product.id)
        assertThat(productLikeCount!!.likeCount).isEqualTo(5L)

        // 2명의 좋아요 제거
        productLikeService.removeProductLike(users[0], product)
        productLikeService.removeProductLike(users[1], product)

        // act - 배치 재실행
        productLikeCountBatchService.reconcileProductLikeCount()

        // assert
        productLikeCount = productLikeCountRepository.findByProductId(product.id)
        assertThat(productLikeCount!!.likeCount).isEqualTo(3L)
    }

    @DisplayName("배치가 대량의 데이터를 처리해도 정확하다")
    @Test
    fun reconcileBatch_handlesLargeVolumeAccurately() {
        // arrange
        val products = (1..10).map { createTestProduct("Brand$it", "Product$it") }
        val users = (1..100).map { createTestUser(it) }

        // 각 상품에 무작위 수의 좋아요 추가
        var userIndex = 0
        products.forEach { product ->
            val likeCount = (5..15).random()
            repeat(likeCount) {
                productLikeService.addProductLike(users[userIndex], product)
                userIndex = (userIndex + 1) % users.size
            }
        }

        // act
        productLikeCountBatchService.reconcileProductLikeCount()

        // assert - 모든 상품의 ProductLikeCount가 저장되었는지 확인
        val productLikeCounts = productLikeCountJpaRepository.findAll()
        assertThat(productLikeCounts).hasSize(10)

        productLikeCounts.forEach { plc ->
            assertThat(plc.likeCount).isGreaterThanOrEqualTo(5L)
            assertThat(plc.likeCount).isLessThanOrEqualTo(15L)
        }
    }
}
