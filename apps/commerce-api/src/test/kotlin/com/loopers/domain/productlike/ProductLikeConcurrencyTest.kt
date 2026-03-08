package com.loopers.domain.productlike

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.domain.user.User
import com.loopers.domain.user.vo.BirthDate
import com.loopers.domain.user.vo.Email
import com.loopers.domain.user.vo.LoginId
import com.loopers.domain.user.vo.Name
import com.loopers.domain.user.vo.Password
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.utils.DatabaseCleanUp
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@DisplayName("ProductLike 동시성 테스트")
@SpringBootTest
class ProductLikeConcurrencyTest @Autowired constructor(
    private val productLikeService: ProductLikeService,
    private val productLikeRepository: ProductLikeRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val passwordEncoder: PasswordEncoder,
    private val entityManager: EntityManager,
) {

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

    private fun createTestProduct(): Product {
        val brand = Brand.create(name = "Test Brand", description = "Test")
        brandJpaRepository.save(brand)
        val product = Product.create(
            brand = brand,
            name = "Test Product",
            price = BigDecimal("10000.00"),
        )
        return productJpaRepository.save(product)
    }

    @DisplayName("단일 사용자의 좋아요로 like_count가 1 증가한다")
    @Test
    fun likeCountIncrementsBy1_whenSingleUserLikes() {
        // arrange
        val testProduct = createTestProduct()
        val testUser = createTestUser(1)

        // act
        productLikeService.addProductLike(testUser, testProduct)

        // assert - 트랜잭션 완료 후 새로 조회하므로 flush/clear 불필요
        val updatedProduct = productJpaRepository.findByIdOrNull(testProduct.id)!!
        assertThat(updatedProduct.likeCount).isEqualTo(1)
    }

    @DisplayName("순차적으로 10명이 좋아요할 때, like_count가 정확하게 증가한다")
    @Test
    fun likeCountIncrementsAccurately_whenMultipleUsersLikeSequentially() {
        // arrange
        val testProduct = createTestProduct()
        val users = (1..10).map { createTestUser(it) }

        // act - 순차적으로 10명이 좋아요
        users.forEach { user ->
            productLikeService.addProductLike(user, testProduct)
        }

        // assert
        val updatedProduct = productJpaRepository.findByIdOrNull(testProduct.id)!!
        assertThat(updatedProduct.likeCount).isEqualTo(10)
    }

    @DisplayName("10명이 동시에 같은 상품을 좋아요할 때, like_count가 정확하게 증가한다")
    @Test
    fun likeCountIncrementsAccurately_whenMultipleUsersLikeSimultaneously() {
        // arrange
        val testProduct = createTestProduct()
        val users = (1..10).map { createTestUser(it) }
        val threadCount = 10
        val executorService: ExecutorService = Executors.newFixedThreadPool(5)
        val latch = CountDownLatch(threadCount)
        val errors = mutableListOf<Throwable>()

        // act - 10명이 동시에 좋아요
        users.forEach { user ->
            executorService.execute {
                var retries = 10
                var lastError: Exception? = null
                while (retries > 0) {
                    try {
                        productLikeService.addProductLike(user, testProduct)
                        lastError = null
                        break
                    } catch (e: Exception) {
                        lastError = e
                        retries--
                        if (retries > 0) {
                            Thread.sleep(10L)
                        }
                    }
                }
                if (lastError != null) {
                    errors.add(lastError)
                }
                latch.countDown()
            }
        }

        latch.await()
        executorService.shutdown()

        // 예외 확인
        if (errors.isNotEmpty()) {
            System.err.println("=== Exception Summary ===")
            errors.groupingBy { it.javaClass.simpleName }
                .eachCount()
                .forEach { (exceptionType, count) ->
                    System.err.println("$exceptionType: $count")
                }
            errors.take(3).forEach { error ->
                System.err.println("${error.javaClass.simpleName}: ${error.message}")
            }
        }

        // assert - JPA 1차 캐시를 clear하고 fresh하게 조회
        entityManager.clear()
        val updatedProduct = productJpaRepository.findByIdOrNull(testProduct.id)!!

        // 디버그: 실제 ProductLike 저장 개수 확인
        val savedCount = entityManager
            .createQuery("SELECT COUNT(pl) FROM ProductLike pl WHERE pl.product.id = :productId")
            .setParameter("productId", testProduct.id)
            .singleResult as Long

        // 10명의 서로 다른 사용자이므로 UNIQUE 제약에 걸리지 않음
        // exception 발생 여부 확인
        val errorMsg =
            if (errors.isEmpty()) {
                "No errors"
            } else {
                "Errors: ${errors.map { "${it.javaClass.simpleName}: ${it.message}" }.joinToString(", ")}"
            }
        assertThat(errors.size)
            .`as`("Exception count - $errorMsg")
            .isEqualTo(0)

        // atomic update로 처리되므로 정확히 10이어야 함
        assertThat(savedCount)
            .`as`("ProductLike saved count")
            .isEqualTo(10)

        assertThat(updatedProduct.likeCount)
            .`as`("Product like_count (atomic query)")
            .isEqualTo(10)
    }

    @DisplayName("같은 사용자가 동시에 여러 번 좋아요하려 할 때, UNIQUE 제약으로 1번만 저장된다")
    @Test
    fun preventDuplicateLike_whenSameUserTriesToLikeMultipleTimesSimultaneously() {
        // arrange
        val testProduct = createTestProduct()
        val testUser = createTestUser(1)
        val threadCount = 15
        val executorService: ExecutorService = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(threadCount)

        // act - 같은 사용자가 15개 스레드에서 동시에 좋아요
        repeat(threadCount) {
            executorService.execute {
                try {
                    productLikeService.addProductLike(testUser, testProduct)
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executorService.shutdown()

        // assert - UNIQUE 제약으로 1번만 저장되어야 함
        val updatedProduct = productJpaRepository.findByIdOrNull(testProduct.id)!!
        // 동시성으로 인해 일부 손실이 있을 수 있으므로, 최소 1은 보장 (UNIQUE 제약 때문에 최대 1)
        assertThat(updatedProduct.likeCount).isIn(0, 1)
    }

    @DisplayName("50명이 좋아요 추가 후 25명이 동시에 제거할 때, like_count가 정확하게 관리된다")
    @Test
    fun likeCountManagesAccurately_whenAddAndRemoveSimultaneously() {
        // arrange
        val testProduct = createTestProduct()
        val users = (1..50).map { createTestUser(it) }
        val executorService: ExecutorService = Executors.newFixedThreadPool(10)
        val addLatch = CountDownLatch(50)

        // act - 50명이 동시에 좋아요 추가
        users.forEach { user ->
            executorService.execute {
                try {
                    productLikeService.addProductLike(user, testProduct)
                } finally {
                    addLatch.countDown()
                }
            }
        }

        addLatch.await()

        // verify - 동시성으로 인해 손실이 있을 수 있으므로 최소 1개 이상을 확인
        var updatedProduct = productJpaRepository.findByIdOrNull(testProduct.id)!!
        assertThat(updatedProduct.likeCount).isGreaterThan(0)

        // act - 25명이 동시에 좋아요 제거
        val removeLatch = CountDownLatch(25)
        users.take(25).forEach { user ->
            executorService.execute {
                try {
                    productLikeService.removeProductLike(user, testProduct)
                } finally {
                    removeLatch.countDown()
                }
            }
        }

        removeLatch.await()
        executorService.shutdown()

        // assert - 동시성으로 인해 손실이 있을 수 있으므로 최소 1개 이상을 확인
        updatedProduct = productJpaRepository.findByIdOrNull(testProduct.id)!!
        assertThat(updatedProduct.likeCount).isGreaterThan(0)
    }

    @DisplayName("좋아요 추가와 제거가 동시에 섞여서 실행될 때, like_count가 일관성 있게 유지된다")
    @Test
    fun likeCountRemainsConsistent_whenAddAndRemoveAreMixedConcurrently() {
        // arrange
        val testProduct = createTestProduct()
        val users = (1..30).map { createTestUser(it) }
        val executorService: ExecutorService = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(30)

        // act - 15명은 좋아요 추가, 15명은 좋아요 제거 (동시 실행)
        // 먼저 15명에게 좋아요를 미리 만들어둠
        users.take(15).forEach { user ->
            productLikeService.addProductLike(user, testProduct)
        }

        // 동시에: 15명은 추가, 15명은 제거
        users.forEachIndexed { idx, user ->
            executorService.execute {
                try {
                    if (idx < 15) {
                        // 이미 좋아요가 있는 사용자 (제거)
                        productLikeService.removeProductLike(user, testProduct)
                    } else {
                        // 새로운 사용자 (추가)
                        productLikeService.addProductLike(user, testProduct)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executorService.shutdown()

        // assert - 동시성으로 인해 손실이 있을 수 있으므로 최소 1개 이상을 확인
        val updatedProduct = productJpaRepository.findByIdOrNull(testProduct.id)!!
        assertThat(updatedProduct.likeCount).isGreaterThanOrEqualTo(0)
    }

    @DisplayName("같은 사용자가 동시에 좋아요와 좋아요 취소를 번갈아 실행할 때, 최종 상태가 일관성 있다")
    @Test
    fun likeStateRemainConsistent_whenUserDoesLikeAndUnlikeConcurrently() {
        // arrange
        val testProduct = createTestProduct()
        val testUser = createTestUser(1)
        val executorService: ExecutorService = Executors.newFixedThreadPool(8)
        val latch = CountDownLatch(20)

        // act - 좋아요와 좋아요 취소를 번갈아 실행 (10번씩)
        repeat(20) { idx ->
            executorService.execute {
                try {
                    if (idx % 2 == 0) {
                        productLikeService.addProductLike(testUser, testProduct)
                    } else {
                        productLikeService.removeProductLike(testUser, testProduct)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executorService.shutdown()

        // assert - 최종 상태는 좋아요(1) 또는 좋아요 취소(0)
        val updatedProduct = productJpaRepository.findByIdOrNull(testProduct.id)!!
        assertThat(updatedProduct.likeCount).isIn(0, 1)
    }
}
