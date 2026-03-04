package com.loopers.concurrency

import com.loopers.application.order.OrderFacade
import com.loopers.application.product.ProductFacade
import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductInventoryService
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
class StockCountDownLatchConcurrencyTest @Autowired constructor(
    private val orderFacade: OrderFacade,
    private val productFacade: ProductFacade,
    private val brandService: BrandService,
    private val productInventoryService: ProductInventoryService,
    private val userJpaRepository: UserJpaRepository,
    private val passwordEncryptor: PasswordEncryptor,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() = databaseCleanUp.truncateAllTables()

    @Test
    fun `재고보다 많은 동시 주문 시 성공 수 = 재고 수이며 최종 재고는 0이다`() {
        val loginId = "testuser"
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
            quantity = 5L,
        )

        val totalThreads = 10
        val latch = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(totalThreads)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        repeat(totalThreads) {
            executor.submit {
                latch.await()
                try {
                    orderFacade.createOrder(
                        loginId = loginId,
                        items = listOf(OrderFacade.OrderItemRequest(productId = product.id, quantity = 1L)),
                        couponId = null,
                    )
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                }
            }
        }

        latch.countDown()
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)

        assertThat(successCount.get()).isEqualTo(5)
        assertThat(failCount.get()).isEqualTo(5)
        val inventory = productInventoryService.getInventory(product.id)
        assertThat(inventory.stock.value).isEqualTo(0L)
    }
}
