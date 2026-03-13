package com.loopers.concurrency

import com.loopers.application.user.order.OrderCreateCommand
import com.loopers.application.user.order.OrderCreateUseCase
import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.coupon.IssuedCouponRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStock
import com.loopers.domain.product.ProductStockRepository
import com.loopers.domain.user.User
import com.loopers.domain.user.UserPasswordHasher
import com.loopers.domain.user.UserRepository
import com.loopers.support.error.CoreException
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZonedDateTime

@DisplayName("주문 트랜잭션 롤백 검증")
@SpringBootTest
class OrderTransactionRollbackTest
@Autowired
constructor(
    private val orderCreateUseCase: OrderCreateUseCase,
    private val productRepository: ProductRepository,
    private val productStockRepository: ProductStockRepository,
    private val brandRepository: BrandRepository,
    private val couponRepository: CouponRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
    private val userRepository: UserRepository,
    private val passwordHasher: UserPasswordHasher,
    private val databaseCleanUp: DatabaseCleanUp,
    private val txManager: PlatformTransactionManager,
) {
    companion object {
        private const val ADMIN = "loopers.admin"
    }

    private var userId: Long = 0
    private var productId: Long = 0

    @BeforeEach
    fun setUp() {
        val user = User.register(
            loginId = "rollbacktest",
            rawPassword = "Password1!",
            name = "테스트",
            birthDate = LocalDate.of(1990, 1, 1),
            email = "rollback@test.com",
            passwordHasher = passwordHasher,
        )
        userId = userRepository.save(user).id!!

        val brand = brandRepository.save(Brand.register(name = "테스트브랜드"), ADMIN)
        val activeBrand = brandRepository.save(brand.update("테스트브랜드", "ACTIVE"), ADMIN)

        val product = Product.register(
            name = "롤백 테스트 상품",
            regularPrice = Money(BigDecimal.valueOf(10000)),
            sellingPrice = Money(BigDecimal.valueOf(10000)),
            brandId = activeBrand.id!!,
        )
        val saved = productRepository.save(product, ADMIN)
        productId = productRepository.save(saved.activate(), ADMIN).id!!

        productStockRepository.save(
            ProductStock.create(productId = productId, initialQuantity = Quantity(10)),
            ADMIN,
        )
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    @DisplayName("이미 사용된 쿠폰으로 주문 시 실패하고, 재고가 원복된다")
    fun create_usedCoupon_stockRolledBack() {
        val coupon = couponRepository.save(
            Coupon.register(
                name = "롤백 테스트 쿠폰",
                type = Coupon.Type.FIXED,
                discountValue = 1000,
                minOrderAmount = null,
                expiredAt = ZonedDateTime.now().plusDays(30),
            ),
        )
        val issuedCoupon = issuedCouponRepository.save(
            IssuedCoupon.issue(
                couponId = coupon.id!!,
                userId = userId,
                expiredAt = coupon.expiredAt,
            ),
        )
        val txTemplate = TransactionTemplate(txManager)
        txTemplate.execute {
            val usedCoupon = issuedCoupon.use()
            issuedCouponRepository.use(usedCoupon)
        }

        val stockBefore = productStockRepository.findByProductId(productId)!!

        assertThatThrownBy {
            orderCreateUseCase.create(
                OrderCreateCommand(
                    userId = userId,
                    idempotencyKey = "rollback-test-1",
                    items = listOf(OrderCreateCommand.Item(productId, 1)),
                    issuedCouponId = issuedCoupon.id!!,
                ),
            )
        }.isInstanceOf(CoreException::class.java)

        val stockAfter = productStockRepository.findByProductId(productId)!!
        assertThat(stockAfter.quantity.value).isEqualTo(stockBefore.quantity.value)
    }
}
