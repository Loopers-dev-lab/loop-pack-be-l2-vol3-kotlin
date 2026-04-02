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
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.utils.DatabaseCleanUp
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.crypto.password.PasswordEncoder
import java.math.BigDecimal
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("ProductLike 동시성 테스트")
@SpringBootTest
class ProductLikeConcurrencyTest @Autowired constructor(
    private val productLikeService: ProductLikeService,
    private val productLikeRepository: ProductLikeRepository,
    private val productLikeCountRepository: ProductLikeCountRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val passwordEncoder: PasswordEncoder,
    private val entityManager: EntityManager,
) {
    @MockBean
    private lateinit var outboxPublisher: OutboxPublisher

    private fun countAuthoritativeLikes(productId: Long): Long =
        entityManager
            .createQuery("SELECT COUNT(pl) FROM ProductLike pl WHERE pl.product.id = :productId")
            .setParameter("productId", productId)
            .singleResult as Long

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

    @DisplayName("단일 사용자의 좋아요로 ProductLike 행이 저장된다")
    @Test
    fun productLikeCreated_whenSingleUserLikes() {
        // arrange
        val testProduct = createTestProduct()
        val testUser = createTestUser(1)

        // act
        productLikeService.addProductLike(testUser, testProduct)

        // assert - like count projection은 commerce-streamer 책임
        assertThat(countAuthoritativeLikes(testProduct.id)).isEqualTo(1)
    }

    @DisplayName("순차적으로 10명이 좋아요할 때, 10개의 ProductLike 행이 저장된다")
    @Test
    fun productLikeCreated_whenMultipleUsersLikeSequentially() {
        // arrange
        val testProduct = createTestProduct()
        val users = (1..10).map { createTestUser(it) }

        // act - 순차적으로 10명이 좋아요
        users.forEach { user ->
            productLikeService.addProductLike(user, testProduct)
        }

        // assert - like count projection은 commerce-streamer 책임
        assertThat(countAuthoritativeLikes(testProduct.id)).isEqualTo(10)
    }

    @DisplayName("순차 add/remove 시 ProductLike 행이 정확히 증감한다")
    @Test
    fun productLikeUpdatesExactly_whenDeterministicAddAndRemoveSequence() {
        // arrange
        val testProduct = createTestProduct()
        val users = (1..10).map { createTestUser(it) }

        users.forEach { user ->
            productLikeService.addProductLike(user, testProduct)
        }

        assertThat(countAuthoritativeLikes(testProduct.id)).isEqualTo(10)

        users.take(4).forEach { user ->
            productLikeService.removeProductLike(user, testProduct)
        }

        // assert - like count projection은 commerce-streamer 책임
        assertThat(countAuthoritativeLikes(testProduct.id)).isEqualTo(6)
    }

    @DisplayName("기존 좋아요가 없는 사용자의 unlike는 ProductLike 행을 감소시키지 않는다")
    @Test
    fun noOpUnlikeDoesNotDecrement_whenNoLikeRowExists() {
        // arrange
        val testProduct = createTestProduct()
        val likedUser = createTestUser(1)
        val neverLikedUser = createTestUser(2)

        productLikeService.addProductLike(likedUser, testProduct)
        assertThat(countAuthoritativeLikes(testProduct.id)).isEqualTo(1)

        productLikeService.removeProductLike(neverLikedUser, testProduct)

        // assert - like count projection은 commerce-streamer 책임
        assertThat(countAuthoritativeLikes(testProduct.id)).isEqualTo(1)
    }

    @DisplayName("10명이 동시에 같은 상품을 좋아요할 때, 10개의 ProductLike 행이 저장된다")
    @Test
    fun productLikeSavedAccurately_whenMultipleUsersLikeSimultaneously() {
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

        // 10명의 서로 다른 사용자이므로 UNIQUE 제약에 걸리지 않음
        val errorMsg =
            if (errors.isEmpty()) {
                "No errors"
            } else {
                "Errors: ${errors.map { "${it.javaClass.simpleName}: ${it.message}" }.joinToString(", ")}"
            }
        assertThat(errors.size)
            .`as`("Exception count - $errorMsg")
            .isEqualTo(0)

        // 정확히 10개의 ProductLike 행이 저장되어야 함
        assertThat(countAuthoritativeLikes(testProduct.id))
            .`as`("ProductLike saved count")
            .isEqualTo(10)
    }

    @DisplayName("같은 사용자가 동시에 여러 번 좋아요하려 할 때, UNIQUE 제약으로 1번만 저장된다")
    @Test
    fun preventDuplicateLike_whenSameUserTriesToLikeMultipleTimesSimultaneously() {
        val testProduct = createTestProduct()
        val testUser = createTestUser(1)
        val threadCount = 15
        val executorService: ExecutorService = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val errors = Collections.synchronizedList(mutableListOf<Throwable>())

        repeat(threadCount) {
            executorService.execute {
                try {
                    productLikeService.addProductLike(testUser, testProduct)
                    successCount.incrementAndGet()
                } catch (t: Throwable) {
                    errors.add(t)
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executorService.shutdown()
        entityManager.clear()

        // assert - like count projection은 commerce-streamer 책임
        assertThat(successCount.get()).isEqualTo(1)
        assertThat(errors.size).isEqualTo(threadCount - 1)
        assertThat(countAuthoritativeLikes(testProduct.id)).isEqualTo(1)
    }

    @DisplayName("50명이 좋아요 추가 후 25명이 동시에 제거할 때, ProductLike 행이 정확하게 관리된다")
    @Test
    fun productLikeManagesAccurately_whenAddAndRemoveSimultaneously() {
        val testProduct = createTestProduct()
        val users = (1..50).map { createTestUser(it) }
        val executorService: ExecutorService = Executors.newFixedThreadPool(10)
        val addLatch = CountDownLatch(50)
        val addErrors = Collections.synchronizedList(mutableListOf<Throwable>())

        users.forEach { user ->
            executorService.execute {
                try {
                    productLikeService.addProductLike(user, testProduct)
                } catch (t: Throwable) {
                    addErrors.add(t)
                } finally {
                    addLatch.countDown()
                }
            }
        }

        addLatch.await()
        entityManager.clear()

        assertThat(addErrors).isEmpty()
        assertThat(countAuthoritativeLikes(testProduct.id)).isEqualTo(50)

        val removeLatch = CountDownLatch(25)
        val removeErrors = Collections.synchronizedList(mutableListOf<Throwable>())
        users.take(25).forEach { user ->
            executorService.execute {
                try {
                    productLikeService.removeProductLike(user, testProduct)
                } catch (t: Throwable) {
                    removeErrors.add(t)
                } finally {
                    removeLatch.countDown()
                }
            }
        }

        removeLatch.await()
        executorService.shutdown()
        entityManager.clear()

        // assert - like count projection은 commerce-streamer 책임
        assertThat(removeErrors).isEmpty()
        assertThat(countAuthoritativeLikes(testProduct.id)).isEqualTo(25)
    }

    @DisplayName("좋아요 추가와 제거가 동시에 섞여서 실행될 때, ProductLike 행이 일관성 있게 유지된다")
    @Test
    fun productLikeRemainsConsistent_whenAddAndRemoveAreMixedConcurrently() {
        val testProduct = createTestProduct()
        val users = (1..30).map { createTestUser(it) }
        val executorService: ExecutorService = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(30)
        val errors = Collections.synchronizedList(mutableListOf<Throwable>())

        users.take(15).forEach { user ->
            productLikeService.addProductLike(user, testProduct)
        }

        users.forEachIndexed { idx, user ->
            executorService.execute {
                try {
                    if (idx < 15) {
                        productLikeService.removeProductLike(user, testProduct)
                    } else {
                        productLikeService.addProductLike(user, testProduct)
                    }
                } catch (t: Throwable) {
                    errors.add(t)
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executorService.shutdown()
        entityManager.clear()

        // assert - like count projection은 commerce-streamer 책임
        assertThat(errors).isEmpty()
        assertThat(countAuthoritativeLikes(testProduct.id)).isEqualTo(15)
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

        // assert - 최종 상태는 일관성이 유지되어야 함 (동시성으로 인해 정확한 값은 보장 불가)
        // ProductLike 행 수는 0 또는 1 (마지막 작업이 add면 1, remove면 0)
        val authoritativeCount = countAuthoritativeLikes(testProduct.id)
        assertThat(authoritativeCount).isLessThanOrEqualTo(1)
    }
}
