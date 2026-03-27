package com.loopers.application.user.like

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.like.ProductLikeRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.context.event.ApplicationEvents
import org.springframework.test.context.event.RecordApplicationEvents
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

@DisplayName("UserProductLike UseCase 통합 테스트")
@RecordApplicationEvents
@SpringBootTest
class UserProductLikeUseCaseIntegrationTest
@Autowired
constructor(
    private val registerUseCase: UserProductLikeRegisterUseCase,
    private val cancelUseCase: UserProductLikeCancelUseCase,
    private val productRepository: ProductRepository,
    private val productLikeRepository: ProductLikeRepository,
    private val brandRepository: BrandRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ADMIN = "loopers.admin"
        private const val USER_ID = 1L
    }

    private var productId: Long = 0

    @Autowired
    lateinit var applicationEvents: ApplicationEvents

    @BeforeEach
    fun setUp() {
        val brand = brandRepository.save(Brand.register(name = "테스트브랜드"), ADMIN)
        val activeBrand = brandRepository.save(brand.update("테스트브랜드", "ACTIVE"), ADMIN)

        val product = Product.register(
            name = "통합테스트 상품",
            regularPrice = Money(BigDecimal.valueOf(10000)),
            sellingPrice = Money(BigDecimal.valueOf(10000)),
            brandId = activeBrand.id!!,
        )
        val saved = productRepository.save(product, ADMIN)
        productId = productRepository.save(saved.activate(), ADMIN).id!!
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    @DisplayName("register 성공")
    inner class WhenRegisterSuccess {
        @Test
        @DisplayName("register 후 product_like row=1, ProductLikeRegisteredEvent가 기록된다")
        fun register_success_rowAndEvent() {
            registerUseCase.register(UserProductLikeCommand.Register(userId = USER_ID, productId = productId))

            val events = applicationEvents.stream(ProductLikeRegisteredEvent::class.java).toList()

            assertThat(productLikeRepository.countByProductId(productId)).isEqualTo(1)
            assertThat(events).hasSize(1)
            assertThat(events.single().productId).isEqualTo(productId)
            assertThat(events.single().userId).isEqualTo(USER_ID)
        }
    }

    @Nested
    @DisplayName("cancel 성공")
    inner class WhenCancelSuccess {
        @Test
        @DisplayName("register 후 cancel 시 product_like row=0, ProductLikeCanceledEvent가 기록된다")
        fun cancel_success_rowAndEvent() {
            registerUseCase.register(UserProductLikeCommand.Register(userId = USER_ID, productId = productId))
            cancelUseCase.cancel(UserProductLikeCommand.Cancel(userId = USER_ID, productId = productId))

            val registeredEvents = applicationEvents.stream(ProductLikeRegisteredEvent::class.java).toList()
            val canceledEvents = applicationEvents.stream(ProductLikeCanceledEvent::class.java).toList()

            assertThat(productLikeRepository.countByProductId(productId)).isEqualTo(0)
            assertThat(registeredEvents).hasSize(1)
            assertThat(canceledEvents).hasSize(1)
            assertThat(canceledEvents.single().productId).isEqualTo(productId)
            assertThat(canceledEvents.single().userId).isEqualTo(USER_ID)
        }
    }

    @Nested
    @DisplayName("cancel no-op")
    inner class WhenCancelNoOp {
        @Test
        @DisplayName("like row 없을 때 cancel 시 row 변화 없음, 취소 이벤트도 없다")
        fun cancel_noOp_rowAndNoEvent() {
            cancelUseCase.cancel(UserProductLikeCommand.Cancel(userId = USER_ID, productId = productId))

            val canceledEvents = applicationEvents.stream(ProductLikeCanceledEvent::class.java).toList()

            assertThat(productLikeRepository.countByProductId(productId)).isEqualTo(0)
            assertThat(canceledEvents).isEmpty()
        }
    }
}
