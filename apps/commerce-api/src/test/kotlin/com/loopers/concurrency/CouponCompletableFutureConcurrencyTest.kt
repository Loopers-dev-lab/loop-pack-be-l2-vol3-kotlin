package com.loopers.concurrency

import com.loopers.application.order.OrderFacade
import com.loopers.application.product.ProductFacade
import com.loopers.domain.brand.BrandService
import com.loopers.domain.coupon.CouponTemplateService
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.UserCouponService
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
import java.time.ZonedDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class CouponCompletableFutureConcurrencyTest @Autowired constructor(
    private val orderFacade: OrderFacade,
    private val productFacade: ProductFacade,
    private val brandService: BrandService,
    private val couponTemplateService: CouponTemplateService,
    private val userCouponService: UserCouponService,
    private val userJpaRepository: UserJpaRepository,
    private val passwordEncryptor: PasswordEncryptor,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() = databaseCleanUp.truncateAllTables()

    private fun createUser(loginId: String) = userJpaRepository.save(
        UserModel(
            loginId = LoginId(loginId),
            encryptedPassword = passwordEncryptor.encrypt("Password123!"),
            name = Name("홍길동"),
            birthDate = BirthDate("1990-01-01"),
            email = Email("$loginId@example.com"),
        )
    )

    private fun createBrand() = brandService.createBrand(
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

    @Test
    fun `동일 쿠폰 동시 사용 시 하나만 성공한다`() {
        val loginId = "testuser"
        val user = createUser(loginId)
        val brand = createBrand()
        val product = productFacade.createProduct(
            brandId = brand.id,
            name = "Air Max",
            imageUrl = "image.png",
            description = "설명",
            price = 50_000L,
            quantity = 100L,
        )
        val template = couponTemplateService.createTemplate(
            name = "테스트쿠폰",
            type = CouponType.FIXED,
            value = 1_000L,
            minOrderAmount = null,
            expiredAt = ZonedDateTime.now().plusDays(1),
        )
        val userCoupon = userCouponService.issueCoupon(user.id, template.id)

        val threads = 2
        val executor = Executors.newFixedThreadPool(threads)
        val starter = CompletableFuture<Void>()
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        val futures = (1..threads).map {
            starter.thenRunAsync({
                try {
                    orderFacade.createOrder(
                        loginId = loginId,
                        items = listOf(OrderFacade.OrderItemRequest(productId = product.id, quantity = 1L)),
                        couponId = userCoupon.id,
                    )
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                }
            }, executor)
        }

        starter.complete(null)
        CompletableFuture.allOf(*futures.toTypedArray()).join()
        executor.shutdown()

        assertThat(successCount.get()).isEqualTo(1)
        assertThat(failCount.get()).isEqualTo(1)
    }

    @Test
    fun `동일 쿠폰 동시 발급 시 하나만 성공한다`() {
        val user = createUser("testuser")
        val template = couponTemplateService.createTemplate(
            name = "테스트쿠폰",
            type = CouponType.FIXED,
            value = 1_000L,
            minOrderAmount = null,
            expiredAt = ZonedDateTime.now().plusDays(1),
        )

        val threads = 2
        val executor = Executors.newFixedThreadPool(threads)
        val starter = CompletableFuture<Void>()
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        val futures = (1..threads).map {
            starter.thenRunAsync({
                try {
                    userCouponService.issueCoupon(user.id, template.id)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                }
            }, executor)
        }

        starter.complete(null)
        CompletableFuture.allOf(*futures.toTypedArray()).join()
        executor.shutdown()

        assertThat(successCount.get()).isEqualTo(1)
        assertThat(failCount.get()).isEqualTo(1)
    }
}
