package com.loopers.concurrency

import com.loopers.application.like.LikeFacade
import com.loopers.application.product.ProductFacade
import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductService
import com.loopers.domain.user.BirthDate
import com.loopers.domain.user.Email
import com.loopers.domain.user.LoginId
import com.loopers.domain.user.Name
import com.loopers.domain.user.PasswordEncryptor
import com.loopers.domain.user.UserModel
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class LikeCountDownLatchConcurrencyTest @Autowired constructor(
    private val likeFacade: LikeFacade,
    private val productFacade: ProductFacade,
    private val brandService: BrandService,
    private val productService: ProductService,
    private val userJpaRepository: UserJpaRepository,
    private val passwordEncryptor: PasswordEncryptor,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() = databaseCleanUp.truncateAllTables()

    @Test
    fun `서로 다른 유저 10명이 같은 상품에 동시 좋아요 시 likeCount는 10이다`() {
        val totalThreads = 10
        val loginIds = (1..totalThreads).map { "user$it" }

        loginIds.forEach { loginId ->
            userJpaRepository.save(
                UserModel(
                    loginId = LoginId(loginId),
                    encryptedPassword = passwordEncryptor.encrypt("Password123!"),
                    name = Name("홍길동"),
                    birthDate = BirthDate("1990-01-01"),
                    email = Email("$loginId@example.com"),
                )
            )
        }

        val brand = brandService.createBrand(
            name = "Nike",
            logoImageUrl = "test.png",
            description = "테스트 브랜드",
            zipCode = "12345",
            roadAddress = "서울특별시 중구 테스트길 1",
            detailAddress = "1층",
            email = "nike@google.com",
            phoneNumber = "02-3783-4401",
            businessNumber = "123-45-67890",
        )

        val product = productFacade.createProduct(
            brandId = brand.id,
            name = "Air Max",
            imageUrl = "image.png",
            description = "설명",
            price = 50_000L,
            quantity = 100L,
        )

        val latch = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(totalThreads)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        loginIds.forEach { loginId ->
            executor.submit {
                latch.await()
                try {
                    likeFacade.like(loginId, product.id)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                }
            }
        }

        latch.countDown()
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)

        assertThat(successCount.get()).isEqualTo(totalThreads)
        assertThat(failCount.get()).isEqualTo(0)

        val updatedProduct = productService.getProductById(product.id)
        assertThat(updatedProduct.likeCount.value).isEqualTo(totalThreads.toLong())
    }

    @Test
    fun `같은 유저가 동시에 좋아요 취소 시 likeCount는 0이다`() {
        val loginId = "user1"
        userJpaRepository.save(
            UserModel(
                loginId = LoginId(loginId),
                encryptedPassword = passwordEncryptor.encrypt("Password123!"),
                name = Name("홍길동"),
                birthDate = BirthDate("1990-01-01"),
                email = Email("$loginId@example.com"),
            )
        )

        val brand = brandService.createBrand(
            name = "Nike",
            logoImageUrl = "test.png",
            description = "테스트 브랜드",
            zipCode = "12345",
            roadAddress = "서울특별시 중구 테스트길 1",
            detailAddress = "1층",
            email = "nike@google.com",
            phoneNumber = "02-3783-4401",
            businessNumber = "123-45-67890",
        )

        val product = productFacade.createProduct(
            brandId = brand.id,
            name = "Air Max",
            imageUrl = "image.png",
            description = "설명",
            price = 50_000L,
            quantity = 100L,
        )

        likeFacade.like(loginId, product.id) // likeCount = 1 세팅

        val threads = 2
        val latch = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(threads)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        repeat(threads) {
            executor.submit {
                latch.await()
                try {
                    likeFacade.unlike(loginId, product.id)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                }
            }
        }

        latch.countDown()
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)

        assertThat(successCount.get()).isEqualTo(2)
        assertThat(failCount.get()).isEqualTo(0)

        val updatedProduct = productService.getProductById(product.id)
        assertThat(updatedProduct.likeCount.value).isEqualTo(0L)
    }
}
